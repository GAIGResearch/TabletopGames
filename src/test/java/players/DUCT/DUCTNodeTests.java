package players.DUCT;

import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.ActionStats;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Focused tests for the DUCT tree. Cover the bits that are easy to get wrong: the decoupled
 * per-player stats at a simultaneous node, that backup only credits the acting player, and that
 * the final move selection is most-visited and stays within the legal actions.
 */
public class DUCTNodeTests {

    private DUCTParams params() {
        return new DUCTParams();
    }

    // build a fresh, set-up 3 player SushiGo game (a simultaneous turn, player 0 to move)
    private Game sushiGo(long seed) {
        Game g = GameType.SushiGo.createGameInstance(3, seed);
        g.reset(Arrays.asList(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        return g;
    }

    private DUCTNode rootFor(Game g) {
        DUCTNode root = new DUCTNode(params(), g.getForwardModel(), g.getGameState(), new Random(0));
        root.updateForOpenLoopState(root.state);
        return root;
    }

    @Test
    public void decoupledStatsAtSimultaneousNode() {
        // run a real search and check the node holds its own stats for every simultaneous player
        DUCTParams p = params();
        p.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        p.setParameterValue("budget", 100);

        Game g = GameType.SushiGo.createGameInstance(3, 404);
        g.reset(Arrays.asList(new BasicDUCTPlayer(p), new RandomPlayer(), new RandomPlayer()));
        BasicDUCTPlayer used = (BasicDUCTPlayer) g.getPlayers().get(0);

        AbstractGameState obs = g.getGameState().copy(0);
        used.getAction(obs, g.getForwardModel().computeAvailableActions(obs));

        DUCTNode root = used.getRoot();
        assertNotNull(root);
        // the acting set must be whatever the game state says, not something we guessed
        assertEquals(obs.getCurrentSimultaneousPlayers(), root.actingPlayers);
        assertTrue("a simultaneous turn should have more than one player deciding",
                root.actingPlayers.size() > 1);
        assertTrue("each simultaneous player should have its own stats at the node",
                root.playerActionStats.size() > 1);
    }

    @Test
    public void actingPlayersFollowGameStateWhenSequential() {
        // TicTacToe reports a single current player, so the node must be single player
        DUCTParams p = params();
        p.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        p.setParameterValue("budget", 50);

        Game g = GameType.TicTacToe.createGameInstance(2, 11);
        g.reset(Arrays.asList(new BasicDUCTPlayer(p), new RandomPlayer()));
        BasicDUCTPlayer used = (BasicDUCTPlayer) g.getPlayers().get(0);

        AbstractGameState obs = g.getGameState().copy(0);
        used.getAction(obs, g.getForwardModel().computeAvailableActions(obs));

        DUCTNode root = used.getRoot();
        assertEquals(obs.getCurrentSimultaneousPlayers(), root.actingPlayers);
        assertEquals(1, root.actingPlayers.size());
    }

    @Test
    public void backUpCreditsOnlyTheActingPlayer() {
        DUCTNode root = rootFor(sushiGo(55));
        AbstractAction a0 = root.playerActionStats.get(0).keySet().iterator().next();

        // one-node path: at the root, player 0 played a0
        root.currentNodeTrajectory.clear();
        root.currentActionTrajectory.clear();
        root.currentNodeTrajectory.add(root);
        root.currentActionTrajectory.add(Collections.singletonMap(0, a0));

        root.backUp(new double[]{5.0, 1.0, 2.0});

        ActionStats stats = root.playerActionStats.get(0).get(a0);
        assertEquals(1, stats.nVisits);
        assertEquals(5.0, stats.totValue[0], 1e-9);
        assertEquals(1.0, stats.totValue[1], 1e-9);
        assertEquals(2.0, stats.totValue[2], 1e-9);
        assertEquals(1, root.nVisits);

        // an action player 0 did not take stays untouched
        for (AbstractAction other : root.playerActionStats.get(0).keySet()) {
            if (!other.equals(a0)) {
                assertEquals(0, root.playerActionStats.get(0).get(other).nVisits);
                break;
            }
        }
    }

    @Test
    public void bestActionPicksMostVisited() {
        DUCTNode root = rootFor(sushiGo(77));
        List<AbstractAction> actions = new ArrayList<>(root.playerActionStats.get(0).keySet());
        assertTrue(actions.size() >= 2);

        AbstractAction favoured = actions.get(0);
        AbstractAction other = actions.get(1);
        root.playerActionStats.get(0).get(favoured).nVisits = 20;
        root.playerActionStats.get(0).get(other).nVisits = 2;

        assertEquals(favoured, root.bestAction(0, actions));
    }

    @Test
    public void bestActionStaysWithinLegalActions() {
        DUCTNode root = rootFor(sushiGo(88));
        List<AbstractAction> actions = new ArrayList<>(root.playerActionStats.get(0).keySet());
        assertTrue(actions.size() >= 2);

        AbstractAction mostVisited = actions.get(0);
        AbstractAction onlyLegal = actions.get(1);
        root.playerActionStats.get(0).get(mostVisited).nVisits = 100;
        root.playerActionStats.get(0).get(onlyLegal).nVisits = 1;

        // even though mostVisited has more visits, only onlyLegal is offered as valid
        assertEquals(onlyLegal, root.bestAction(0, Collections.singletonList(onlyLegal)));
    }

    @Test
    public void getActionReturnsALegalMove() {
        DUCTParams p = params();
        p.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        p.setParameterValue("budget", 50);

        Game g = GameType.SushiGo.createGameInstance(3, 909);
        g.reset(Arrays.asList(new BasicDUCTPlayer(p), new RandomPlayer(), new RandomPlayer()));
        BasicDUCTPlayer used = (BasicDUCTPlayer) g.getPlayers().get(0);

        AbstractGameState obs = g.getGameState().copy(0);
        List<AbstractAction> actions = g.getForwardModel().computeAvailableActions(obs);
        AbstractAction chosen = used.getAction(obs, actions);

        assertTrue("DUCT must return one of the legal actions", actions.contains(chosen));
    }
}
