package players.mcts;

import core.actions.AbstractAction;
import players.mcts.ActionStats;
import players.mcts.SingleTreeNode;

import java.util.List;

public class STNWithTestInstrumentation extends SingleTreeNode {

    @Override
    public double[] actionValues(List<AbstractAction> actions) {
        return super.actionValues(actions);
    }


    public ActionStats getActionStats(AbstractAction action) {
        return actionValues.get(action);
    }

    public AbstractAction treePolicyAction(boolean useExploration) {
        return super.treePolicyAction(useExploration);
    }
}
