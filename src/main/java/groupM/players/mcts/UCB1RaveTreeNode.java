package groupM.players.mcts;

import java.util.Random;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import core.AbstractGameState;

public class UCB1RaveTreeNode extends UCB1TreeNode implements IAmafBackup{
    protected Mean amfMean;

    protected UCB1RaveTreeNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
        super(player, parent, state, rnd);
        amfMean = new Mean();
    }

    @Override
    public void amafBackUp(double result) {
        UCB1RaveTreeNode n = this;
        while (n != null) {
            n.amfMean.increment(result);
            n = (UCB1RaveTreeNode) n.parent;
        }    
    }

    @Override 
    double ucb1(TreeNode child){
        UCB1RaveTreeNode ucbChild = (UCB1RaveTreeNode) child;

        double qValue = ucbChild.mean.getResult();
        double explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (ucbChild.nVisits + player.params.epsilon));
        
        double amafValue = amfMean.getResult();
        double raveValue = RAVE.getValue(this.player.params.amafV, this.nVisits, qValue, amafValue);
        
        boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
        raveValue = iAmMoving ? raveValue : - raveValue;
        
        return raveValue + explorationTerm;
    }

    @Override
    Rollout newRollout(){
        return new AMAFRollout();
    }
}