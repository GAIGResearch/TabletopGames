package groupM.players.mcts;

import java.util.HashSet;
import java.util.Set;

import core.actions.AbstractAction;

class Rollout {
    protected double result;
    protected Set<AbstractAction> rolloutActions = new HashSet<>();

    void addAction(AbstractAction action){
        rolloutActions.add(action);
    }

    void setResult(double result){
        this.result = result;
    }

    void backupRollout(TreeNode selectedNode){
        selectedNode.backUp(result);
    }
}