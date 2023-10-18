package groupM.players.mcts;

import java.util.Random;
import static utilities.Utils.noise;

import core.AbstractGameState;

public class UCB1TreeNode extends TreeNode{
    
    // Total value of this node
    protected double totValue;
    // Number of visits
    protected int nVisits;

    protected UCB1TreeNode(GroupMMCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
        super(player, parent, state, rnd);
        totValue = 0.0;
    }

    void backUp(double result) {

        UCB1TreeNode n = this;
        while (n != null) {
            n.nVisits++;
            n.totValue += result;
            n = (UCB1TreeNode) n.parent;
        }
    }

    @Override
    double getChildValue(TreeNode child, boolean isExpanding) {
         return isExpanding ? ucb1(child) : nVisitsWithNoise(child);
    }


    private double ucb1(TreeNode child) {
        UCB1TreeNode ucbChild = (UCB1TreeNode) child;

        double hvVal = ucbChild.totValue;
        double childValue = hvVal / (ucbChild.nVisits + player.params.epsilon);
        double explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (ucbChild.nVisits + player.params.epsilon));

        // Find 'UCB' value
        // If 'we' are taking a turn we use classic UCB
        // If it is an opponent's turn, then we assume they are trying to minimise our score (with exploration)
        boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
        double uctValue = iAmMoving ? childValue : -childValue;
        uctValue += explorationTerm;

        // Apply small noise to break ties randomly
        uctValue = noise(uctValue, player.params.epsilon, player.rnd.nextDouble());

        return uctValue;
    }
    
    private double nVisitsWithNoise(TreeNode child) {
        UCB1TreeNode ucbChild = (UCB1TreeNode) child;

        double childValue = ucbChild.nVisits;

        // Apply small noise to break ties randomly
        childValue = noise(childValue, player.params.epsilon, player.rnd.nextDouble());
        return childValue;
    }






}
