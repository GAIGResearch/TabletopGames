package groupM.players.mcts;

import java.util.Random;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.util.FastMath;

import core.AbstractGameState;
import umontreal.ssj.probdist.GammaDist;
import umontreal.ssj.probdist.NormalDist;

public class ThompsonTreeNode extends TreeNode{

    private int nVisits;
    private double alpha;
    private double beta;
    private Mean mean;

    protected ThompsonTreeNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
        super(player, parent, state, rnd);
        this.mean = new Mean();
        this.nVisits = 0;
        this.alpha = 0.0;
        this.beta = 0.0;
    }

    @Override
    double getChildValue(TreeNode child, boolean isExpanding) {
        ThompsonTreeNode thomsonChild = (ThompsonTreeNode) child;
        boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();

        // selecting best action -> just return mean of all results
        if(!isExpanding){
            return iAmMoving ? thomsonChild.mean.getResult(): - thomsonChild.mean.getResult();
        }
        
        // tree policy -> thompson sampling
        double value = thompsonSampling(thomsonChild);

        return iAmMoving ? value: - value;
    }

    private double thompsonSampling(ThompsonTreeNode thomsonChild) {
        // sample precision from gamme dist using inverse sampling trick
        double u = player.rnd.nextDouble();
        double precision = GammaDist.inverseF(thomsonChild.alpha, thomsonChild.beta, 5, u);
        
        if(Double.isNaN(precision) || Double.isInfinite(precision)|| precision == 0 || thomsonChild.nVisits == 0){
            precision = 0.001;
        }
        
        // sample value from normal dist using inverse sampling trick
        u = player.rnd.nextDouble();
        double value = NormalDist.inverseF(thomsonChild.mean.getResult(), Math.sqrt(1/precision), u);
        return value;
    }


    @Override
    void backUp(double result) {
        ThompsonTreeNode node = this;
        while (node != null) {
            double n = 1;
            double v = node.nVisits;

            double mu = node.mean.getResult();
            if (Double.isNaN(mu)){
                mu = 0;
            }

            node.alpha += n / 2;
            node.beta += (n*v/(v+n)) * (FastMath.pow(result - mu, 2) / 2);
            node.nVisits += 1;
            node.mean.increment(result);
            
            node = (ThompsonTreeNode) node.parent;
        }
    }
    
}


