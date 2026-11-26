package players.search;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import games.connect4.Connect4ForwardModel;
import games.connect4.Connect4GameParameters;
import games.connect4.Connect4GameState;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class IterativeDeepeningBudgetTests {

    private static final int BUDGET = 200;
    private static final int REPETITIONS = 20;

    private final Connect4ForwardModel forwardModel = new Connect4ForwardModel();
    private Connect4GameState state;
    private int rootActionCount;
    private List<AbstractAction> winningActions;

    /**
     * Scores wins and losses only, and stalls for longer than the budget on one chosen evaluation.
     * This places the deadline at an exact point in the search, independent of how fast the machine is.
     */
    private static class StallingHeuristic implements IStateHeuristic {
        final int stallOnEvaluation;
        int evaluations = 0;

        StallingHeuristic(int stallOnEvaluation) {
            this.stallOnEvaluation = stallOnEvaluation;
        }

        @Override
        public double evaluateState(AbstractGameState gs, int playerId) {
            // each leaf is evaluated once per player, so only count the first of these
            if (playerId == 0 && ++evaluations == stallOnEvaluation) {
                try {
                    Thread.sleep(BUDGET + 100);
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
            return switch (gs.getPlayerResults()[playerId]) {
                case WIN_GAME -> 1.0;
                case LOSE_GAME -> -1.0;
                default -> 0.0;
            };
        }
    }

    @Before
    public void setup() {
        // Player 0 stacks column 0 and Player 1 column 1, leaving Player 0 to move with exactly one winning column
        state = new Connect4GameState(new Connect4GameParameters(), 2);
        forwardModel.setup(state);
        for (int move = 0; move < 6; move++) {
            forwardModel.next(state, forwardModel.computeAvailableActions(state).get(move % 2));
        }
        assertEquals(0, state.getCurrentPlayer());

        List<AbstractAction> actions = forwardModel.computeAvailableActions(state);
        rootActionCount = actions.size();
        winningActions = new ArrayList<>();
        for (AbstractAction action : actions) {
            AbstractGameState copy = state.copy();
            forwardModel.next(copy, action.copy());
            if (copy.getPlayerResults()[0] == CoreConstants.GameResult.WIN_GAME) {
                winningActions.add(action);
            }
        }
        assertEquals(1, winningActions.size());
    }

    private MaxNSearchPlayer createPlayer(int searchDepth, boolean iterativeDeepening, int budget, IStateHeuristic heuristic) {
        MaxNSearchParameters params = new MaxNSearchParameters();
        params.searchDepth = searchDepth;
        params.iterativeDeepening = iterativeDeepening;
        params.expandByEstimatedValue = false;
        params.budget = budget;
        params.budgetType = PlayerConstants.BUDGET_TIME;
        params.heuristic = heuristic;
        MaxNSearchPlayer player = new MaxNSearchPlayer(params);
        player.setForwardModel(forwardModel);
        return player;
    }

    private void assertAlwaysPlaysWin(int searchDepth, int stallOnEvaluation) {
        StallingHeuristic heuristic = new StallingHeuristic(stallOnEvaluation);
        MaxNSearchPlayer player = createPlayer(searchDepth, true, BUDGET, heuristic);
        for (int i = 0; i < REPETITIONS; i++) {
            heuristic.evaluations = 0;
            AbstractAction chosen = player.getAction(state, forwardModel.computeAvailableActions(state));
            assertEquals("Should keep the answer of the deepest completed pass", winningActions.get(0), chosen);
        }
    }

    @Test
    public void passesAfterTheDeadlineDoNotDiscardTheCompletedAnswer() {
        // depth 1 completes, then the deadline passes on the first evaluation of depth 2
        assertAlwaysPlaysWin(8, rootActionCount + 1);
    }

    @Test
    public void finalPassInterruptedDoesNotDiscardTheCompletedAnswer() {
        // as above, but with depth 2 being the last pass, so it is the interrupted result that is at issue
        assertAlwaysPlaysWin(2, rootActionCount + 1);
    }

    @Test
    public void noNewDepthIsStartedOnceTheBudgetIsSpent() {
        StallingHeuristic heuristic = new StallingHeuristic(rootActionCount + 1);
        MaxNSearchPlayer player = createPlayer(8, true, BUDGET, heuristic);
        player.getAction(state, forwardModel.computeAvailableActions(state));
        // all of depth 1, then the single line of depth 2 on which the deadline passed
        assertEquals(rootActionCount + 1, heuristic.evaluations);
    }

    @Test
    public void budgetSmallerThanOnePlyStillReturnsAnAction() {
        StallingHeuristic heuristic = new StallingHeuristic(1);
        MaxNSearchPlayer player = createPlayer(8, true, BUDGET, heuristic);
        List<AbstractAction> actions = forwardModel.computeAvailableActions(state);
        assertTrue(actions.contains(player.getAction(state, actions)));
        assertEquals(1, heuristic.evaluations);
    }

    @Test
    public void searchWithinBudgetMatchesFixedDepthSearch() {
        StallingHeuristic neverStalls = new StallingHeuristic(-1);
        MaxNSearchPlayer iterative = createPlayer(3, true, Integer.MAX_VALUE, neverStalls);
        MaxNSearchPlayer fixedDepth = createPlayer(3, false, Integer.MAX_VALUE, neverStalls);
        iterative.getAction(state, forwardModel.computeAvailableActions(state));
        fixedDepth.getAction(state, forwardModel.computeAvailableActions(state));
        assertArrayEquals(fixedDepth.getRootResult().value(), iterative.getRootResult().value(), 1e-9);
    }
}
