package groupM.players.mcts;

import java.util.Comparator;
import java.util.Random;

import core.actions.AbstractAction;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

import static utilities.Utils.noise;

import core.AbstractGameState;

public class UCB1TreeNode extends TreeNode{
    protected Mean mean;

    protected UCB1TreeNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
        super(player, parent, state, rnd);
        mean = new Mean();
    }

    @Override
    Comparator<TreeNode> getPruningComparator() {
        return Comparator.comparing(c->ucb1(c));
    }

    void backUp(double result) {
        UCB1TreeNode n = this;
        while (n != null) {
            n.nVisits++;
            n.mean.increment(result);
            n = (UCB1TreeNode) n.parent;
        }
    }


    @Override
    double getChildValue(TreeNode child, boolean isExpanding) {
        if(!isExpanding){
            return nVisitsWithNoise(child);
        }
        return ucb1(child);
    }


    double ucb1(TreeNode child) {
        UCB1TreeNode ucbChild = (UCB1TreeNode) child;

        double qValue = ucbChild.mean.getResult();
        double explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (ucbChild.nVisits + player.params.epsilon));
        
        boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
        qValue = iAmMoving ? qValue : - qValue;
        qValue += explorationTerm;

        return noise(qValue, player.params.epsilon, player.rnd.nextDouble());
    }

    
    private double nVisitsWithNoise(TreeNode child) {
        UCB1TreeNode ucbChild = (UCB1TreeNode) child;

        double childValue = ucbChild.nVisits;

        // Apply small noise to break ties randomly
        childValue = noise(childValue, player.params.epsilon, player.rnd.nextDouble());
        return childValue;
    }
}
