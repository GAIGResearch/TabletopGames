package players.mcts;

import core.actions.AbstractAction;
import games.GameType;
import utilities.Pair;

import java.util.List;

import static org.junit.Assert.*;

public class STNRollout extends SingleTreeNode {

    static int lastActorInTree, staticRolloutDepth, staticPenultimateTurn, staticPenultimateRound;
    List<Pair<Integer, AbstractAction>> rolloutActions;

    @Override
    protected void oneSearchIteration() {
        super.oneSearchIteration();

        int expectedRolloutLength = params.rolloutLength;
        if (params.rolloutLengthPerPlayer) {
            expectedRolloutLength *= openLoopState.getNPlayers();
        }

        // Now we can run some tests on this
        if (openLoopState == null || !openLoopState.isNotTerminal())
            return; // don't test terminal states

        assertEquals(staticRolloutDepth, rolloutActions.size());
        switch (params.rolloutTermination) {
            case EXACT:
                // in this case we just check that we have expected actions in the rollout
                assertEquals(expectedRolloutLength, staticRolloutDepth);
                break;
            case START_ACTION:
                // in this case we have at least the expected actions, and finish at the end of a player's Turn
                // which means that the current player should be the MCTS player, and
                // the last player should be someone else
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertEquals(0, openLoopState.getTurnOwner());
                assertNotEquals(0, rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                break;
            case END_ACTION:
                // in this case we have at least expected actions, and finish at the end of a player's Turn
                // which means that the current player is not the same as the player who acted last
                // and the last player who acted should be the decision player
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertNotEquals(openLoopState.getTurnOwner(), rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                assertEquals(0, rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                break;
            case END_TURN:
                // in this case we have at least expected actions, and finish at the end of a player's Turn
                // which means that the turn has *just* changed, so the current turn is one more than the penultimate turn
                // (i.e. the turn just before the last action in the rollout)
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertNotEquals(openLoopState.getTurnCounter(), staticPenultimateTurn);
                assertTrue(openLoopState.getTurnCounter() == staticPenultimateTurn + 1 || openLoopState.getTurnCounter() == 0);
                break;
            case END_ROUND:
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertNotEquals(openLoopState.getRoundCounter(), staticPenultimateRound);
                if (openLoopState.getGameType() == GameType.Poker)
                    assertTrue(openLoopState.getRoundCounter() == staticPenultimateRound + 1 || openLoopState.getRoundCounter() == staticPenultimateRound + 2);
                else
                    assertEquals(openLoopState.getRoundCounter(), staticPenultimateRound + 1);
                break;
        }
    }

    @Override
    protected void updateMASTStatistics(List<Pair<Integer, AbstractAction>> tree, List<Pair<Integer, AbstractAction>> rollout, double[] value) {
        rolloutActions = rollout;
    }

    @Override
    protected double[] rollout(int lastActor) {
        lastActorInTree = lastActor;  // a bit of a hack to track the last Actor in tree search
        double[] retValue = super.rollout(lastActor);
        staticRolloutDepth = root.actionsInRollout.size();
        staticPenultimateTurn = lastTurnInRollout;
        staticPenultimateRound = lastRoundInRollout;
        return retValue;

    }
}
