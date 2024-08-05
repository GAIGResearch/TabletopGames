package players.mcts;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import core.AbstractGameState;
import core.actions.AbstractAction;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
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


    @Override
    public void rootify(SingleTreeNode template, AbstractGameState state) {
        super.rootify(template, state);
        this.OMAParent = Optional.empty();
    }
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
        // We first need to find the action that was taken from this node - which we can get from actionsInTree

        for (int player = 0; player < result.length; player++) {
            if (params.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA && player != root.decisionPlayer)
                continue;  // for OMA we only consider the root player

            // We only care about our actions for OMA stats
            int finalPlayer = player;
            List<AbstractAction> selfActionsOnly = root.actionsInTree.stream()
                    .filter(p -> p.a == finalPlayer).map(p -> p.b)
                    .toList();
            List<OMATreeNode> nodes = new ArrayList<>();
            OMATreeNode currentNode = this;
            do {
                if (currentNode.decisionPlayer == player && currentNode.nVisits > 0)
                    nodes.add(currentNode);
                currentNode = (OMATreeNode) currentNode.parent;
            } while (currentNode != null); // we have reached root
            Collections.reverse(nodes); // and reverse so root is at the start
            // We now have a list of nodes for player - this should be the same length as the list of actions
            if (nodes.size() != selfActionsOnly.size())
                throw new AssertionError("We should have the same number of nodes as actions");
            if (nodes.isEmpty())
                continue;
            // We now go down the sequence of actions taken from the root
            for (int i = 0; i < selfActionsOnly.size() - 1; i++) {
                currentNode = nodes.get(i);
                AbstractAction actionTakenFromParent = selfActionsOnly.get(i);
                AbstractAction actionTakenFromChild = selfActionsOnly.get(i + 1);
                if (currentNode.decisionPlayer != player)
                    throw new AssertionError("We have a mismatch between the player who took the action and the player who should be acting");
                if (!currentNode.actionValues.containsKey(actionTakenFromParent))
                    throw new AssertionError("We should not have a value for the action taken from the parent");
                currentNode.OMABackup(result, actionTakenFromParent, actionTakenFromChild);
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
