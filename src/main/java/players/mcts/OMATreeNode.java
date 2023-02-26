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
    // We do not need to store arrays of statistics (one element per player), as we only ever
    // consider OMA stats from the perspective of the acting player.
    // However, we need to store one set of stats for each action we take from the OMAParent node,
    // as it is the 'grandchildren' of this action that are amalgamated to give the OMA stats.
    // The first Action key is the action from the OMAParent node. The second Action key is the action at the
    // grandchildren.
    final Map<AbstractAction, Map<AbstractAction, OMAStats>> OMAChildren = new HashMap<>();

    /**
     * Back up the value of the child through all parents. Increase number of visits and total value.
     *
     * @param result - value of rollout to backup
     */
    @Override
    protected void backUp(double[] result) {
        super.backUp(result); // first we do the usual stuff
        // and now we add on the OMA statistics
        // These are updated on the node from which an action was taken (the OMAParent)
        // and not the node that is reached by taking an action.
        // We first need to find the action that was taken from this node.
        // - This will be actionToReach on the next node in the trajectory
        // - or null for the very last node (the one expanded)...for the moment we do not treat the first
        // rollout action as the 'action taken'

        for (int player = 0; player < result.length; player++) {
            if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA && player != root.decisionPlayer)
                continue;  // for OMA we only consider the root player
            // We loop over each player separately to avoid confusion with interleaving actions
            // it might be more efficient to do this in a single pass...but at the cost of greater algorithmic complexity and bugginess
            OMATreeNode iteratingNode = this;
            AbstractAction actionTakenFromChild = null;
            // we also need the action taken from the OMAParent to get here. To find this we need to back-track up
            // the nodes and find the actionToReach on the immediate child of OMAParent

            // the aim is to find the first node on the backwards trajectory that has an OMAParent
            while (iteratingNode != null) {
                if (iteratingNode.decisionPlayer == player && iteratingNode.OMAParent.isPresent()) {
                    // Now find the action taken FROM the parent
                    if (actionTakenFromChild != null) {
                        OMATreeNode OMAParent = iteratingNode.OMAParent.get();
                        OMATreeNode parentSearch = iteratingNode;
                        AbstractAction actionTakenFromParent;
                        do { // we now head up to the OMAParent, recording the action taken from it
                            actionTakenFromParent = parentSearch.actionToReach; // this is the action just before we find the OMAParent present
                            parentSearch = (OMATreeNode) parentSearch.parent;
                            if (parentSearch == null)
                                throw new AssertionError("We should be guaranteed to find OMAParent before reaching root");
                        } while (parentSearch != OMAParent);
                        OMAParent.OMABackup(result, actionTakenFromParent, actionTakenFromChild);
                    }
                    // we now continue from iterating node for BP further up the tree
                }
                actionTakenFromChild = iteratingNode.actionToReach;
                iteratingNode = (OMATreeNode) iteratingNode.parent;
            }
        }
    }

    private void OMABackup(double[] result, AbstractAction actionTakenFromParent, AbstractAction actionTakenToReachChild) {
        if (!OMAChildren.containsKey(actionTakenFromParent))
            OMAChildren.put(actionTakenFromParent, new HashMap<>());
        Map<AbstractAction, OMAStats> possibleActions = OMAChildren.get(actionTakenFromParent);
        if (!possibleActions.containsKey(actionTakenToReachChild))
            possibleActions.put(actionTakenToReachChild, new OMAStats());
        OMAStats stats = possibleActions.get(actionTakenToReachChild);
        stats.OMAVisits++;
        stats.OMATotValue += result[decisionPlayer];
    }

    @Override
    protected void instantiate(SingleTreeNode parent, AbstractAction actionToReach, AbstractGameState state) {
        super.instantiate(parent, actionToReach, state);
        // We only track OMAParents for all players if using OMA_All; otherwise just for the root decision player
        if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA_All || root.decisionPlayer == decisionPlayer) {
            SingleTreeNode oneUp = this.parent;
            while (oneUp != null) {
                if (oneUp.decisionPlayer == decisionPlayer) {
                    // found it..we're done
                    this.OMAParent = Optional.of((OMATreeNode) oneUp);
                    break;
                }
                oneUp = oneUp.parent;
            }
        }
    }

    public Optional<OMATreeNode> getOMAParent() {
        return OMAParent;
    }

    public OMAStats getOMAStats(AbstractAction action1, AbstractAction action2) {
        return OMAChildren.get(action1).get(action2);
    }

    public Set<AbstractAction> getOMAChildrenActions(AbstractAction action) {
        return OMAChildren.getOrDefault(action, new HashMap<>()).keySet();
    }

    public Set<AbstractAction> getOMAParentActions() {
        return OMAChildren.keySet();
    }
}
