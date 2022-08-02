package players.mcts;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import core.AbstractGameState;
import core.actions.AbstractAction;
import utilities.Pair;

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

    public class OMAStats {
        public double OMATotValue;
        public int OMAVisits;
    }


    protected Optional<OMATreeNode> OMAParent = Optional.empty(); // the most recent parent of this node when it was the node player action
    // this is the node that will have the OMA statistics on for the current action

    // we then have a separate copy of all the standard statistics maintained for SingleTreeNode
    // when making a decision at *this* node, we use the statistics held on the OMAParent node
    // We also do not need to store Arrays (one element per player) of values and successor nodes, as
    // these are always going to be for the same player as this node
    final Map<AbstractAction, OMAStats> OMAChildren = new HashMap<>();

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to backup
     */
    @Override
    protected void backUp(double[] result, List<Pair<Integer, AbstractAction>> rolloutActions) {
        super.backUp(result, rolloutActions); // first we do the usual stuff
        // and now we add on the OMA statistics
        // These are update on the node from which an action was taken (the OMAParent)
        // and not the node that is reached by taking an action.
        // We first need to find the action that was taken from this node.
        // - This will be actionToReach on the next node in the trajectory
        // - or null for the very last node (the one expanded)...for the moment we do not treat the first
        // rollout action as the 'action taken'
        OMATreeNode n = this;
        AbstractAction actionTaken = null;
        while (n != null) {
            // we jump for players out of scope
            if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA_All || n.decisionPlayer == root.decisionPlayer) {
                if (actionTaken != null && n.OMAParent.isPresent()) {
                    n.OMAParent.get().OMABackup(result, actionTaken);
                }
            }
            actionTaken = n.actionToReach;
            n = (OMATreeNode) n.parent;
        }
    }

    private void OMABackup(double[] result, AbstractAction actionTaken) {
        if (!OMAChildren.containsKey(actionTaken))
            OMAChildren.put(actionTaken, new OMAStats());
        OMAStats stats = OMAChildren.get(actionTaken);
        stats.OMAVisits++;
        stats.OMATotValue += result[decisionPlayer];
    }

    @Override
    protected SingleTreeNode expandNode(AbstractAction actionCopy, AbstractGameState nextState) {
        OMATreeNode retValue = (OMATreeNode) super.expandNode(actionCopy, nextState);
        // Now we need to link this up to its OMAParent (if such exists)
        // we follow the links up the tree to find the closest one at which we last acted

        // We only track OMAParents for all players if using OMA_All; otherwise just for the root decision player
        if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA_All || root.decisionPlayer == retValue.decisionPlayer) {
            SingleTreeNode oneUp = retValue.parent;
            while (oneUp != null && !retValue.OMAParent.isPresent()) {
                if (oneUp.decisionPlayer == retValue.decisionPlayer) {
                    // found it..we're done
                    retValue.OMAParent = Optional.of((OMATreeNode) oneUp);
                }
                oneUp = oneUp.parent;
            }
        }
        return retValue;
    }

    public Optional<OMATreeNode> getOMAParent() {
        return OMAParent;
    }

    public OMAStats getOMAStats(AbstractAction action) {
        return OMAChildren.get(action);
    }

    public Set<AbstractAction> getOMAChildrenActions() {
        return OMAChildren.keySet();
    }
}
