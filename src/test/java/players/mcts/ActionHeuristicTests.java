package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import org.junit.Test;
import players.PlayerConstants;
import players.heuristics.NullActionHeuristic;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class ActionHeuristicTests {

    TicTacToeForwardModel fm = new TicTacToeForwardModel();

    /** Prefers actions later in the list of available actions. */
    private static final IActionHeuristic LATER_IS_BETTER =
            (action, state, contextActions) -> contextActions.indexOf(action);

    private MCTSParams params(int budget, int seed) {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", budget);
        params.setParameterValue("randomSeed", seed);
        return params;
    }

    private Game ticTacToe(AbstractPlayer mcts) {
        Game game = GameType.TicTacToe.createGameInstance(2, 404);
        game.reset(List.of(mcts, new RandomPlayer(new Random(17))));
        return game;
    }

    /** The MCTS player decides, then it and the random player each make a move, and the MCTS player decides again. */
    private void twoDecisions(MCTSPlayer player) {
        Game game = ticTacToe(player);
        AbstractGameState state = game.getGameState();
        AbstractAction first = player.getAction(state, fm.computeAvailableActions(state));
        fm.next(state, first);
        List<AbstractAction> replies = fm.computeAvailableActions(state);
        fm.next(state, replies.get(0));
        player.getAction(state, fm.computeAvailableActions(state));
    }

    private void assertMASTHeuristicReadsTheLiveStatistics(MCTSPlayer player) {
        assertTrue(player.getParameters().actionHeuristic instanceof MASTActionHeuristic);
        MASTActionHeuristic heuristic = (MASTActionHeuristic) player.getParameters().actionHeuristic;
        assertSame(player.root.MASTStatistics, heuristic.getMASTStats());
        assertFalse(heuristic.getMASTStats().get(0).isEmpty());
    }

    private MCTSParams MASTParams() {
        MCTSParams params = params(200, 42);
        params.setParameterValue("useMASTAsActionHeuristic", true);
        return params;
    }

    @Test
    public void MASTHeuristicReadsTheLiveStatisticsInASingleTree() {
        MCTSPlayer player = new MCTSPlayer(MASTParams());
        twoDecisions(player);
        assertMASTHeuristicReadsTheLiveStatistics(player);
    }

    @Test
    public void MASTHeuristicReadsTheDecayedStatisticsWithMASTGamma() {
        MCTSParams params = MASTParams();
        params.setParameterValue("MASTGamma", 0.5);
        MCTSPlayer player = new MCTSPlayer(params);
        twoDecisions(player);
        assertMASTHeuristicReadsTheLiveStatistics(player);
    }

    @Test
    public void MASTHeuristicReadsTheLiveStatisticsWhenTheTreeIsReused() {
        MCTSParams params = MASTParams();
        params.setParameterValue("reuseTree", true);
        params.setParameterValue("discardStateAfterEachIteration", false);
        MCTSPlayer player = new MCTSPlayer(params);
        twoDecisions(player);
        assertTrue("the tree was not reused", player.root.inheritedVisits > 0);
        assertMASTHeuristicReadsTheLiveStatistics(player);
    }

    @Test
    public void MASTHeuristicReadsTheLiveStatisticsInMultiTree() {
        MCTSParams params = MASTParams();
        params.setParameterValue("opponentTreePolicy", MCTSEnums.OpponentTreePolicy.MultiTree);
        MCTSPlayer player = new MCTSPlayer(params);
        twoDecisions(player);
        assertTrue(player.root instanceof MultiTreeNode);
        assertMASTHeuristicReadsTheLiveStatistics(player);
    }

    @Test
    public void MASTHeuristicReadsTheLiveStatisticsWithSeveralDeterminisations() {
        MCTSParams params = MASTParams();
        params.setParameterValue("numDeterminizations", 3);
        MCTSPlayer player = new MCTSPlayer(params);
        twoDecisions(player);
        assertTrue(player.root instanceof ForestNode);
        assertMASTHeuristicReadsTheLiveStatistics(player);
    }

    private void decide(MCTSParams params) {
        MCTSPlayer player = new MCTSPlayer(params);
        Game game = ticTacToe(player);
        AbstractGameState state = game.getGameState();
        player.getAction(state, fm.computeAvailableActions(state));
    }

    @Test
    public void pUCTWithNoActionHeuristicFails() {
        MCTSParams params = params(50, 42);
        params.setParameterValue("pUCTTemperature", 1.0);
        assertThrows(AssertionError.class, () -> decide(params));
        assertThrows(AssertionError.class, params::instantiate);
    }

    @Test
    public void progressiveBiasWithANullActionHeuristicFromJSONFails() {
        MCTSParams params = params(50, 42);
        params.setParameterValue("actionHeuristic", new NullActionHeuristic());
        params.setParameterValue("progressiveBias", 0.5);
        assertThrows(AssertionError.class, () -> decide(params));
        assertThrows(AssertionError.class, params::instantiate);
    }

    @Test
    public void pUCTWithAnActionHeuristicIsLeftOn() {
        MCTSParams params = params(50, 42);
        params.setParameterValue("actionHeuristic", LATER_IS_BETTER);
        params.setParameterValue("pUCTTemperature", 1.0);
        MCTSPlayer player = new MCTSPlayer(params);
        Game game = ticTacToe(player);
        AbstractGameState state = game.getGameState();
        player.getAction(state, fm.computeAvailableActions(state));
        assertEquals(1.0, player.getParameters().pUCTTemperature, 0.0);
    }

    @Test
    public void moveOrderingExpandsTheActionsTheHeuristicPrefersFirst() {
        for (int seed = 1; seed <= 5; seed++) {
            MCTSParams params = params(3, seed);
            params.setParameterValue("actionHeuristic", LATER_IS_BETTER);
            MCTSPlayer player = new MCTSPlayer(params);
            Game game = ticTacToe(player);
            AbstractGameState state = game.getGameState();
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            player.getAction(state, actions);
            for (int i = 0; i < actions.size(); i++)
                assertEquals("seed " + seed + ", action " + i, i >= actions.size() - 3,
                        player.root.actionVisits(actions.get(i)) > 0);
        }
    }

    @Test
    public void actionHeuristicRecalculationDefaultsTo20() {
        MCTSParams params = new MCTSParams();
        assertEquals(20, params.getParameterValue("actionHeuristicRecalculation"));
        params.setParameterValue("budget", 100);
        assertEquals(20, params.actionHeuristicRecalculationThreshold);
    }
}
