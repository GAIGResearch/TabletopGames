package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import evaluation.listeners.ActionFeatureListener;
import evaluation.listeners.StateFeatureListener;
import evaluation.metrics.Event;

import java.util.*;

public class MCTSExpertIterationListener extends ActionFeatureListener {

    public int visitThreshold, maxDepth;
    MCTSPlayer oracle;

    /**
     * This Listener is used to record the statistics of the MCTS tree during expert iteration.
     * <p>
     * For some moves it runs a full MCTS tree search, and records the statistics of the root node.
     * This represents the state value (V), the action values (Q), and the visit counts for each action
     * according to the oracle agent.
     */
    public MCTSExpertIterationListener(MCTSPlayer oracle, IActionFeatureVector actionFeatures, IStateFeatureVector stateFeatures,
                                       int visitThreshold, int maxDepth) {
        super(actionFeatures, stateFeatures, Event.GameEvent.ACTION_CHOSEN, true);
        this.visitThreshold = visitThreshold;
        this.oracle = oracle;
        this.maxDepth = maxDepth;
    }

    @Override
    public void processState(AbstractGameState state, AbstractAction action) {
        // we override this from FeatureListener, because we want to record the feature vector for each action
        // we record this once, and cache the results for all actions

        if (action == null) return; // we do not record data for the GAME_OVER event
        List<AbstractAction> availableActions = game.getForwardModel().computeAvailableActions(state);
        if (availableActions.size() == 1) {
            // only one action available, so no decision to take
            return;
        }

        oracle.setForwardModel(game.getForwardModel());
        oracle.getAction(state, availableActions);
        recordData(oracle.root);

        // TODO: then we also record the state features
    }

    public MCTSPlayer getOracle() {
        return oracle;
    }

    public void recordData(SingleTreeNode root) {

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
            List<AbstractAction> actionsFromState = game.getForwardModel().computeAvailableActions(node.state);
            actionValues = new HashMap<>();
            actionValues.put("CHOSEN", new HashMap<>());
            actionValues.put("VISIT_PROPORTION", new HashMap<>());
            actionValues.put("ADVANTAGE", new HashMap<>());
            actionValues.put("ACTION_VALUE", new HashMap<>());
            actionValues.put("STATE_VALUE", new HashMap<>());
            actionValues.put("DEPTH", new HashMap<>());
            actionValues.put("NODE_VISITS", new HashMap<>());
            actionValues.put("ACTION_VISITS", new HashMap<>());
            actionValues.put("ACTIONS_TOTAL", new HashMap<>());
            actionValues.put("PLAYER", new HashMap<>());

            AbstractAction bestAction = null;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (AbstractAction action : actionsFromState) {
                actionValues.get("DEPTH").put(action, (double) node.depth);
                actionValues.get("NODE_VISITS").put(action, (double) node.getVisits());
                actionValues.get("ACTION_VISITS").put(action, (double) node.actionVisits(action));
                actionValues.get("ACTIONS_TOTAL").put(action, (double) actionsFromState.size());
                actionValues.get("PLAYER").put(action, (double) player);
                if (node.actionValues.get(action) == null) {
                    actionValues.get("CHOSEN").put(action, 0.0);  // we have no data for this action
                    actionValues.get("VISIT_PROPORTION").put(action, 0.0);  // we have no data for this action
                    actionValues.get("ADVANTAGE").put(action, 0.0);  // we have no data for this action
                    actionValues.get("ACTION_VALUE").put(action, 0.0);  // we have no data for this action
                    continue;
                }
                double actionValue = node.actionTotValue(action, player) / node.actionVisits(action);
                actionValues.get("ACTION_VALUE").put(action, actionValue);
                if (actionValue > bestValue) {
                    bestValue = actionValue;
                    bestAction = action;
                }
                actionValues.get("CHOSEN").put(action, 0.0);
                double visitProportion = (double) node.actionVisits(action) / node.getVisits();
                actionValues.get("VISIT_PROPORTION").put(action, visitProportion);
                actionValues.get("ADVANTAGE").put(action, actionValue - stateValue);

            }

            // the best action is the highest scoring one in the available set (which may not be the best one overall away from the root)
            actionValues.get("CHOSEN").put(bestAction, 1.0);

            if (actionsFromState.size() > 1) {
                // the super class will then pull in the feature vectors for state and all actions
                // and populate the main currentData map (then held until the end of the game so it can be updated with final scores)
                super.processState(node.state, bestAction);
                // TODO: having done that, we can now record *just* the state value
            }

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
