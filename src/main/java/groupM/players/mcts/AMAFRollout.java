package groupM.players.mcts;

import core.actions.AbstractAction;

class AMAFRollout extends Rollout{
    @Override
    void backupRollout(TreeNode selectedNode){
        super.backupRollout(selectedNode);
        
        if(selectedNode.parent == null){
            return;
        }
      
        // amaf backup siblings of node that were visited during rollout
        for(AbstractAction action : selectedNode.parent.children.keySet()){
            if(rolloutActions.contains(action)){
                TreeNode node = selectedNode.parent.children.get(action);
                 if (node instanceof IAmafBackup){
                    IAmafBackup amafNode = (IAmafBackup) node;

                    if(amafNode != null){
                        amafNode.amafBackUp(result);
                    }
                 }
            }
        }
        
    }
}