package players.search;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import games.connect4.Connect4ForwardModel;
import games.connect4.Connect4GameParameters;
import games.connect4.Connect4GameState;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;

import java.util.List;

import static org.junit.Assert.*;

/**
 * MaxNSearchPlayer must not ask the forward model for the actions of a state it will not search:
 * some games (LoveLetter, SushiGo, Poker) throw when asked for the actions of a terminal state.
 */
public class MaxNTerminalStateTests {

    /**
     * Behaves as LoveLetter etc. do on a terminal state, and counts the calls made to it.
     */
    private static class StrictForwardModel extends Connect4ForwardModel {
        int calls = 0;

        @Override
        protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
            calls++;
            if (!gameState.isNotTerminal())
                throw new AssertionError("Actions requested for a terminal state");
            return super._computeAvailableActions(gameState);
        }
    }

    private final StrictForwardModel forwardModel = new StrictForwardModel();
    private Connect4GameState state;
    private AbstractAction winningAction;

    @Before
    public void setup() {
        // Player 0 stacks column 0 and Player 1 column 1, leaving Player 0 to move with exactly one winning column
        state = new Connect4GameState(new Connect4GameParameters(), 2);
        forwardModel.setup(state);
        for (int move = 0; move < 6; move++) {
            forwardModel.next(state, forwardModel.computeAvailableActions(state).get(move % 2));
        }
        assertEquals(0, state.getCurrentPlayer());
        for (AbstractAction action : forwardModel.computeAvailableActions(state)) {
            AbstractGameState copy = state.copy();
            forwardModel.next(copy, action.copy());
            if (copy.getPlayerResults()[0] == CoreConstants.GameResult.WIN_GAME) {
                winningAction = action;
            }
        }
        assertNotNull(winningAction);
    }

    private MaxNSearchPlayer createPlayer(int searchDepth) {
        MaxNSearchParameters params = new MaxNSearchParameters();
        params.searchDepth = searchDepth;
        params.budget = Integer.MAX_VALUE;
        params.budgetType = PlayerConstants.BUDGET_TIME;
        MaxNSearchPlayer player = new MaxNSearchPlayer(params);
        player.setForwardModel(forwardModel);
        return player;
    }

    @Test
    public void searchThatEndsTheGameDoesNotListActionsOfTheFinalState() {
        MaxNSearchPlayer player = createPlayer(2);
        AbstractAction chosen = player.getAction(state, forwardModel.computeAvailableActions(state));
        assertEquals(winningAction, chosen);
    }

    @Test
    public void depthOneSearchListsNoActionsBelowTheRoot() {
        MaxNSearchPlayer player = createPlayer(1);
        List<AbstractAction> rootActions = forwardModel.computeAvailableActions(state);
        forwardModel.calls = 0;
        player.getAction(state, rootActions);
        // the children of the root are only evaluated, so their actions are never needed
        assertEquals(0, forwardModel.calls);
    }
}
