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
    protected ActionTargetType actionTarget;

    public enum ActionTargetType {
        CHOSEN,
        VISIT_PROPORTION,
        ADVANTAGE
    }

    /**
     * Two sets of features are provided.
     *
     * @param stateFeatures  - the features that will be record to value a state (V)
     * @param actionFeatures - the additional features used for Q
     */
    public MCTSTreeActionStatisticsListener(IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures,
                                            int visitThreshold, int maxDepth, ActionTargetType actionTarget) {
        super(actionFeatures, stateFeatures, Event.GameEvent.ACTION_CHOSEN, true);
        this.actionTarget = actionTarget;
        this.visitThreshold = visitThreshold;
        this.maxDepth = maxDepth;
    }


    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ACTION_CHOSEN) {
            // We extract the root node from the current player's tree
            AbstractPlayer player = this.getGame().getPlayers().get(event.state.getCurrentPlayer());
            if (player instanceof MCTSPlayer) {
                MCTSPlayer mctsPlayer = (MCTSPlayer) player;
                recordData(mctsPlayer.root, this.getGame().getForwardModel());
            }
        } else if (event.type == Event.GameEvent.GAME_OVER) {
            writeDataWithStandardHeaders(event.state);
        }
        // else we do nothing
    }

    public void recordData(SingleTreeNode root, AbstractForwardModel forwardModel) {

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
            Map<AbstractAction, Double> actionTargets = new HashMap<>();
            AbstractAction bestAction = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (AbstractAction action : actionsFromState) {
                if (node.actionValues.get(action) == null) {
                    actionTargets.put(action, 0.0);  // we have no data for this action
                    continue;
                }
                double actionValue = node.actionTotValue(action, player) / node.actionVisits(action);
                if (actionValue > bestValue) {
                    bestValue = actionValue;
                    bestAction = action;
                }
                switch (actionTarget) {
                    case CHOSEN:
                        actionTargets.put(action, 0.0);
                        break;
                    case VISIT_PROPORTION:
                        double visitProportion = (double) node.actionVisits(action) / node.getVisits();
                        actionTargets.put(action, visitProportion);
                        break;
                    case ADVANTAGE:
                        actionTargets.put(action, actionValue - stateValue);
                        break;
                }
            }
            // the best action is the highest scoring one in the available set (which may not be the best one overall away from the root)
            if (actionTarget == ActionTargetType.CHOSEN)
                actionTargets.put(bestAction, 1.0);

            processStateWithTargets(node.state, bestAction, actionTargets);

            // add children of current node to queue if they meet the criteria
            for (SingleTreeNode child : node.children.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .filter(n -> n.depth <= maxDepth)
                    .filter(n -> n.getVisits() >= visitThreshold)
                    .collect(toList())) {
                if (child != null)
                    nodeQueue.add(child);
            }

        }
    }
}
