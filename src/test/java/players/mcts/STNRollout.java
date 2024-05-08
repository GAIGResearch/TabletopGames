package players.mcts;

import core.actions.AbstractAction;
import players.mcts.SingleTreeNode;
import utilities.Pair;

import java.util.List;

import static org.junit.Assert.*;

public class STNRollout extends SingleTreeNode {

    static int lastActorInTree, staticRolloutDepth, staticStartTurn, staticStartRound;
    List<Pair<Integer, AbstractAction>> rolloutActions;

    @Override
    protected void oneSearchIteration() {
        super.oneSearchIteration();

        // Now we can run some tests on this
        if (openLoopState == null || !openLoopState.isNotTerminal())
            return; // don't test terminal states

        assertEquals(staticRolloutDepth, rolloutActions.size());
        switch (params.rolloutTermination) {
            case DEFAULT:
                // in this case we just check that we have 10 actions in the rollout
                assertEquals(10, staticRolloutDepth);
                break;
            case START_TURN:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player should be the MCTS player, and
                // the last player should be someone else
                assertTrue(staticRolloutDepth >= 10);
                assertEquals(0, openLoopState.getTurnOwner());
                assertNotEquals(0, rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                break;
            case END_TURN:
                // in this case we have at least 10 actions, and finish at the end of a player's Turn
                // which means that the current player is not the same as the player who acted last
                // and the last player who acted should be the decision player
                assertTrue(staticRolloutDepth >= 10);
                assertNotEquals(openLoopState.getTurnOwner(), rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                assertEquals(0, rolloutActions.get(rolloutActions.size() - 1).a.intValue());
                break;
            case END_ROUND:
                assertTrue(staticRolloutDepth >= 10);
                assertNotEquals(openLoopState.getRoundCounter(), staticStartRound);
                break;
        }
    }

    @Override
    protected void updateMASTStatistics(List<Pair<Integer, AbstractAction>> tree, List<Pair<Integer, AbstractAction>> rollout, double[] value) {
        rolloutActions = rollout;
    }

    @Override
    protected double[] rollout(double[] startingValues, int lastActor) {
        lastActorInTree = lastActor;  // a bit of a hack to track the last Actor in tree search
        double[] retValue = super.rollout(startingValues, lastActor);
        staticRolloutDepth = rolloutDepth;
        staticStartRound = roundAtStartOfRollout;
        staticStartTurn = turnAtStartOfRollout;
        return retValue;

    }
}
