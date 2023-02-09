package test.players.mcts;

import core.actions.AbstractAction;
import players.mcts.SingleTreeNode;
import utilities.Pair;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class STNRollout extends SingleTreeNode {

    static int lastActorInTree, staticRolloutDepth;
    List<Pair<Integer, AbstractAction>> rolloutActions;

    @Override
    protected void oneSearchIteration() {
        super.oneSearchIteration();
        // convenience data for debugging
//        lastRolloutData = new RolloutData(rolloutDepth, roundAtStartOfRollout, openLoopState.getRoundCounter(),
//                turnAtStartOfRollout, openLoopState.getTurnCounter(),
//                lastActorInTree, openLoopState.getCurrentPlayer(), rolloutActions);

        // Now we can run some tests on this
        if (openLoopState == null || !openLoopState.isNotTerminal())
            return; // don't test terminal states
        switch (params.rolloutTermination) {
            case DEFAULT:
                assertEquals(10, rolloutActions.size());
                assertEquals(10, staticRolloutDepth);
                break;
            case START_TURN:
                break;
            case END_TURN:
                break;
            case END_ROUND:
                break;
        }
    }

    @Override
    protected void updateMASTStatistics(List<Pair<Integer, AbstractAction>> tree, List<Pair<Integer, AbstractAction>> rollout, double[] value) {
        rolloutActions = rollout;
    }

    @Override
    protected double[] rollout(List<Pair<Integer, AbstractAction>> rolloutActions, double[] startingValues, int lastActor) {
        lastActorInTree = lastActor;  // a bit of a hack to track the last Actor in tree search
        double[] retValue = super.rollout(rolloutActions, startingValues, lastActor);
        staticRolloutDepth = rolloutDepth;
        return retValue;

    }
}
