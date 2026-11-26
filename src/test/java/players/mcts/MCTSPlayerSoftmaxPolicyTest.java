package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.DominionParameters;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * {@link MCTSPlayer#getSoftmaxPolicy} is the fork's addition for comparing agent policies; the merge rewrote the
 * visit-count lookup it relies on ({@code actionValues} moved into a per-player record), so it is pinned here.
 */
public class MCTSPlayerSoftmaxPolicyTest {

    private TestMCTSPlayer mctsPlayer;
    private MCTSParams params;
    private Game game;

    @Before
    public void setup() {
        params = new MCTSParams();
        params.setRandomSeed(9332);
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 50;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.K = 1.0;

        mctsPlayer = new TestMCTSPlayer(params, null);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));

        DominionParameters dp = new DominionParameters();
        dp.setRandomSeed(330245);
        game = new Game(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(dp, 3));
    }

    /** Advances the game to the first decision point that offers a real choice (the opener has only one action). */
    private List<AbstractAction> actionsAtAChoice(AbstractGameState[] holder) {
        AbstractGameState state = game.getGameState();
        for (int i = 0; i < 60 && state.isNotTerminal(); i++) {
            List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
            if (actions.size() >= 2) {
                holder[0] = state;
                return actions;
            }
            if (actions.isEmpty()) break;
            game.getForwardModel().next(state, actions.get(0).copy());
        }
        throw new AssertionError("no decision point with more than one action was reached");
    }

    @Test
    public void policyIsAProbabilityDistributionOverTheSuppliedActions() {
        AbstractGameState[] holder = new AbstractGameState[1];
        List<AbstractAction> actions = actionsAtAChoice(holder);
        AbstractGameState state = holder[0];
        assertNotNull(mctsPlayer._getAction(state, actions));

        double[] policy = mctsPlayer.getSoftmaxPolicy(actions, 0.5);

        assertEquals(actions.size(), policy.length);
        assertEquals("policy must be normalised", 1.0, Arrays.stream(policy).sum(), 1e-9);
        for (double p : policy) {
            assertTrue("negative probability " + p, p >= 0.0);
            assertTrue("probability above 1 " + p, p <= 1.0);
        }
    }

    @Test
    public void policyWeightsFollowTheRootVisitCounts() {
        AbstractGameState[] holder = new AbstractGameState[1];
        List<AbstractAction> actions = actionsAtAChoice(holder);
        AbstractGameState state = holder[0];
        mctsPlayer._getAction(state, actions);

        double[] policy = mctsPlayer.getSoftmaxPolicy(actions, 0.5);

        int mostVisited = 0;
        int heaviest = 0;
        for (int i = 1; i < actions.size(); i++) {
            if (mctsPlayer.getRoot().actionVisits(actions.get(i)) > mctsPlayer.getRoot().actionVisits(actions.get(mostVisited)))
                mostVisited = i;
            if (policy[i] > policy[heaviest]) heaviest = i;
        }
        assertTrue("the search should have visited at least one action", mctsPlayer.getRoot().actionVisits(actions.get(mostVisited)) > 0);
        assertEquals("the most visited action must carry the largest policy weight", mostVisited, heaviest);
    }
}
