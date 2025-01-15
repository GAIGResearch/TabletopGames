package players.mcts;

import core.actions.AbstractAction;
import utilities.Pair;

import java.util.List;

import static org.junit.Assert.*;

public class STNRollout extends SingleTreeNode {

    static int lastActorInTree, staticRolloutDepth, staticStartTurn, staticStartRound;
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
            case DEFAULT:
                // in this case we just check that we have 10 actions in the rollout
                assertEquals(expectedRolloutLength, staticRolloutDepth);
                break;
            case START_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player should be the MCTS player, and
                // the last player should be someone else
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertEquals(0, openLoopState.getTurnOwner());
                assertNotEquals(0, rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                break;
            case END_ACTION:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player is not the same as the player who acted last
                // and the last player who acted should be the decision player
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertNotEquals(openLoopState.getTurnOwner(), rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                assertEquals(0, rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                break;
            case END_TURN:
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertNotEquals(openLoopState.getTurnCounter(), staticStartTurn);
                break;
            case END_ROUND:
                assertTrue(staticRolloutDepth >= expectedRolloutLength);
                assertNotEquals(openLoopState.getRoundCounter(), staticStartRound);
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
        staticStartRound = roundAtStartOfRollout;
        staticStartTurn = turnAtStartOfRollout;
        return retValue;

    }
}
