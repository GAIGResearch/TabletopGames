package groupM.players.mcts;

import java.util.Random;
import static utilities.Utils.noise;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class UCB1TreeNode extends TreeNode{
    
    // Total value of this node
    protected double totValue;
    // Number of visits
    protected int nVisits;

    protected UCB1TreeNode(MCTSPlayer player, TreeNode parent, AbstractGameState state, Random rnd) {
        super(player, parent, state, rnd);
        totValue = 0.0;
    }


    @Override
    AbstractAction selectAction() {
     // Find child with highest UCB value, maximising for ourselves and minimizing for opponent
     AbstractAction bestAction = null;
     double bestValue = -Double.MAX_VALUE;

     for (AbstractAction action : children.keySet()) {
        UCB1TreeNode child = (UCB1TreeNode) children.get(action);
         if (child == null)
             throw new AssertionError("Should not be here");
         else if (bestAction == null)
             bestAction = action;

         // Find child value
         double hvVal = child.totValue;
         double childValue = hvVal / (child.nVisits + player.params.epsilon);
         double explorationTerm = player.params.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + player.params.epsilon));

         // Find 'UCB' value
         // If 'we' are taking a turn we use classic UCB
         // If it is an opponent's turn, then we assume they are trying to minimise our score (with exploration)
         boolean iAmMoving = state.getCurrentPlayer() == player.getPlayerID();
         double uctValue = iAmMoving ? childValue : -childValue;
         uctValue += explorationTerm;

         // Apply small noise to break ties randomly
         uctValue = noise(uctValue, player.params.epsilon, player.rnd.nextDouble());

         // Assign value
         if (uctValue > bestValue) {
             bestAction = action;
             bestValue = uctValue;
         }
     }

     if (bestAction == null)
         throw new AssertionError("We have a null value in UCT : shouldn't really happen!");

     root.fmCallsCount++;  // log one iteration complete
     return bestAction;
    }
    
    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to backup
     */
    @Override
    void backUp(double result) {

        UCB1TreeNode n = this;
        while (n != null) {
            n.nVisits++;
            n.totValue += result;
            n = (UCB1TreeNode) n.parent;
        }
    }
    

    @Override
    /**
     * Calculates the best action from the root according to the most visited node
     *
     * @return - the best AbstractAction
     */
    AbstractAction bestAction() {

        double bestValue = -Double.MAX_VALUE;
        AbstractAction bestAction = null;

        for (AbstractAction action : children.keySet()) {
            if (children.get(action) != null) {
                UCB1TreeNode node = (UCB1TreeNode) children.get(action);
                double childValue = node.nVisits;

                // Apply small noise to break ties randomly
                childValue = noise(childValue, player.params.epsilon, player.rnd.nextDouble());

                // Save best value (highest visit count)
                if (childValue > bestValue) {
                    bestValue = childValue;
                    bestAction = action;
                }
            }
        }

        if (bestAction == null) {
            throw new AssertionError("Unexpected - no selection made.");
        }

        return bestAction;
    }

}
