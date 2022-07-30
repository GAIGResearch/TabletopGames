package players.mcts;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.*;

import static players.mcts.MCTSEnums.Information.Closed_Loop;

/**
 * Opponent Move Abstraction adds a set of statistics at each node that ignore (abstract out) the decisions
 * made by other players, and the 'state' of which is defined purely by the sequence of 'our' actions taken to reach this node.
 * <p>
 * Maintenance of these statistics is done during back-propagation independently of the core statistics.
 * <p>
 * They are then used during Selection, interpolating with the standard MCTS Selection calculation
 */
public class OMATreeNode extends SingleTreeNode {

    private class OMAStats {
        public double OMATotValue;
        public double OMATotSquares;
        public int OMAVisits;
    }

    protected Optional<OMATreeNode> OMAParent = Optional.empty(); // the most recent parent of this node when it was the node player action
    // this is the node that will have the OMA statistics on for the current action

    // we then have a separate copy of all the standard statistics maintained for SingleTreeNode
    // when making a decision at *this* node, we use the statistics held on the OMAParent node
    // We also do not need to store Arrays (one element per player) of values and successor nodes, as
    // these are always going to be for the same player as this node
    private final Map<AbstractAction, Integer> OMAValidVisits = new HashMap<>();
    // TODO: We do need to store statistics by AbstractAction, as we need this to calculate the OMA value estimate
    Map<AbstractAction, OMAStats> OMAChildren = new HashMap<>();

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to backup
     */
    @Override
    protected void backUp(double[] result) {
        super.backUp(result); // first we do the usual stuff
        // and now we add on the OMA statistics
        for (int player = 0; player < result.length; player++) {
            if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA && player != root.decisionPlayer)
                continue;
            // We now find the first node (working backwards) for this player, and back-propagate from there
            SingleTreeNode n = this;
            // TODO: Problem - we need to know the action that led from the OMAParent on this trajectory
            // to update the statistics...which seems much better than re-calculating on the fly each time.
            while (n != null) {
                if (n.decisionPlayer == player)
                    OMABackup((OMATreeNode) n, result);
                n = n.parent;
            }
        }
    }

    private void OMABackup(OMATreeNode node, double[] result) {
        if (!node.OMAParent.isPresent())  // possible if this is the first node for the player in trajectory
            return;
        OMATreeNode n = node.OMAParent.get();

        while (n != null) {
            n.OMAVisits++;
            // Here we look at actionsFromOpenLoopState to see which ones were valid
            // when we passed through, and keep track of valid visits
            // This is now also valid for Closed Loop, as these are amalgamated across different states
            for (AbstractAction action : n.actionsFromOpenLoopState) {
                if (!n.OMAValidVisits.containsKey(action))
                    n.OMAValidVisits.put(action, 1);
                else
                    n.OMAValidVisits.put(action, n.OMAValidVisits.get(action) + 1);
            }
            n.OMATotValue += result[decisionPlayer];
            n.OMATotSquares += result[decisionPlayer] * result[decisionPlayer];
            n = n.OMAParent.orElse(null);
        }
    }

    @Override
    protected SingleTreeNode expandNode(AbstractAction actionCopy, AbstractGameState nextState) {
        OMATreeNode retValue = (OMATreeNode) super.expandNode(actionCopy, nextState);
        // Now we need to link this up to its OMAParent (if such exists)
        // we follow the links up the tree to find the closest one at which we last acted

        // We only track OMAParents for all players if using OMA_All; otherwise just for the root decision player
        if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA_All || root.decisionPlayer == decisionPlayer) {
            SingleTreeNode oneUp = retValue.parent;
            while (oneUp != null && !retValue.OMAParent.isPresent()) {
                if (oneUp.decisionPlayer == this.decisionPlayer) {
                    // found it..we're done
                    retValue.OMAParent = Optional.of((OMATreeNode) oneUp);
                }
            }
        }
        return retValue;
    }


}
