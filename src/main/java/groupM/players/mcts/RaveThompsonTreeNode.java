package groupM.players.mcts;

import java.util.Random;

import core.AbstractGameState;

public class RaveThompsonTreeNode extends ThompsonTreeNode implements IAmafBackup {
    private NormalGammaDistribution amafDist; 

    protected RaveThompsonTreeNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
        super(player, parent, state, rnd);
        this.amafDist = new NormalGammaDistribution();
    }
    
    @Override
    public void amafBackUp(double result) {
        RaveThompsonTreeNode node = this;
        while (node != null) {
            node.amafDist.observeSample(result);
            node = (RaveThompsonTreeNode) node.parent;
        }
    }

    @Override
    Rollout newRollout(){
        return new AMAFRollout();
    }

    @Override
    double thompsonSample(ThompsonTreeNode child){
        RaveThompsonTreeNode raveChild = (RaveThompsonTreeNode) child;
        
        double value = child.dist.sample(this.player.rnd);
        double amafValue = raveChild.amafDist.sample(this.player.rnd);
        double raveValue = RAVE.getValue(this.player.params.amafV, child.dist.getNVisits(), value, amafValue);

        return raveValue;
    }
}