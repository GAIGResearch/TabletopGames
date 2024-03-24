package players.mcts;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import evaluation.listeners.ActionFeatureListener;
import evaluation.metrics.Event;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class MCTSTreeActionStatisticsListener extends ActionFeatureListener {

    public int visitThreshold, maxDepth;

    /**
     * Two sets of features are provided.
     *
     * @param stateFeatures  - the features that will be record to value a state (V)
     * @param actionFeatures - the additional features used for Q
     */
    public MCTSTreeActionStatisticsListener(IActionFeatureVector actionFeatures, IStateFeatureVector stateFeatures,
                                            int visitThreshold, int maxDepth, String fileName) {
        super(actionFeatures, stateFeatures, Event.GameEvent.ACTION_CHOSEN, true, fileName);
        this.visitThreshold = visitThreshold;
        this.maxDepth = maxDepth;
    }


    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ACTION_CHOSEN) {
            // We extract the root node from the current player's tree
            AbstractPlayer player = this.getGame().getPlayers().get(event.state.getCurrentPlayer());
            if (player instanceof MCTSPlayer mctsPlayer) {
                recordData(mctsPlayer.root, this.getGame().getForwardModel());
            }
        } else if (event.type == Event.GameEvent.GAME_OVER) {
            writeDataWithStandardHeaders(event.state);
        }
        // else we do nothing
    }

    public void recordData(SingleTreeNode root, AbstractForwardModel forwardModel) {

        if (root instanceof MultiTreeNode) {
            // access the root for the acting player instead
            root = ((MultiTreeNode) root).roots[root.getActor()];
        }

        // Now do our stuff, and trawl through the root to record data
        Queue<SingleTreeNode> nodeQueue = new ArrayDeque<>();
        if (root == null || root.getVisits() < visitThreshold) {
            return;
        }
        nodeQueue.add(root);

        if (maxDepth > 0 && !root.params.maintainMasterState) {
            throw new IllegalArgumentException("maxDepth > 0 requires maintainMasterState to be true");
        }

        while (!nodeQueue.isEmpty()) {
            SingleTreeNode node = nodeQueue.poll();
            // process this node
            // we record its depth, value, visits, and the full feature list
            int player = node.getActor();
            double stateValue = node.nodeValue(player);
            List<AbstractAction> actionsFromState = forwardModel.computeAvailableActions(node.state);
            Map<String, Map<AbstractAction, Double>> actionTargets = new HashMap<>();
            actionTargets.put("CHOSEN", new HashMap<>());
            actionTargets.put("VISIT_PROPORTION", new HashMap<>());
            actionTargets.put("ADVANTAGE", new HashMap<>());
            actionTargets.put("ACTION_VALUE", new HashMap<>());
            actionTargets.put("DEPTH", new HashMap<>());
            actionTargets.put("NODE_VISITS", new HashMap<>());
            actionTargets.put("ACTION_VISITS", new HashMap<>());
            actionTargets.put("ACTIONS_TOTAL", new HashMap<>());
            actionTargets.put("PLAYER", new HashMap<>());

            AbstractAction bestAction = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (AbstractAction action : actionsFromState) {
                actionTargets.get("DEPTH").put(action, (double) node.depth);
                actionTargets.get("NODE_VISITS").put(action, (double) node.getVisits());
                actionTargets.get("ACTION_VISITS").put(action, (double) node.actionVisits(action));
                actionTargets.get("ACTIONS_TOTAL").put(action, (double) actionsFromState.size());
                actionTargets.get("PLAYER").put(action, (double) player);
                if (node.actionValues.get(action) == null) {
                    actionTargets.get("CHOSEN").put(action, 0.0);  // we have no data for this action
                    actionTargets.get("VISIT_PROPORTION").put(action, 0.0);  // we have no data for this action
                    actionTargets.get("ADVANTAGE").put(action, 0.0);  // we have no data for this action
                    actionTargets.get("ACTION_VALUE").put(action, 0.0);  // we have no data for this action
                    continue;
                }
                double actionValue = node.actionTotValue(action, player) / node.actionVisits(action);
                actionTargets.get("ACTION_VALUE").put(action, actionValue);
                if (actionValue > bestValue) {
                    bestValue = actionValue;
                    bestAction = action;
                }
                actionTargets.get("CHOSEN").put(action, 0.0);
                double visitProportion = (double) node.actionVisits(action) / node.getVisits();
                actionTargets.get("VISIT_PROPORTION").put(action, visitProportion);
                actionTargets.get("ADVANTAGE").put(action, actionValue - stateValue);

            }

            // the best action is the highest scoring one in the available set (which may not be the best one overall away from the root)
            actionTargets.get("CHOSEN").put(bestAction, 1.0);

            if (actionsFromState.size() > 1) // no information
                processStateWithTargets(node.state, bestAction, actionTargets);

            // add children of current node to queue if they meet the criteria
            for (SingleTreeNode child : node.children.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .filter(n -> n.depth <= maxDepth)
                    .filter(n -> n.getVisits() >= visitThreshold)
                    .toList()) {
                if (child != null)
                    nodeQueue.add(child);
            }

        }
    }
}
