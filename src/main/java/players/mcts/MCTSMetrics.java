package players.mcts;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;

import java.util.*;
import java.util.stream.Collectors;

public class MCTSMetrics implements IMetricsCollection {


    public static class TreeStats extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractPlayer player = listener.getGame().getPlayers().get(e.state.getCurrentPlayer());
            if (!(player instanceof MCTSPlayer mctsPlayer)) return false;
            SingleTreeNode root = mctsPlayer.root;
            if (root instanceof MultiTreeNode) {
                root = Arrays.stream(((MultiTreeNode) root).roots).filter(Objects::nonNull)
                        .filter(node -> node.decisionPlayer == e.state.getCurrentPlayer())
                        .findFirst().orElse(null);
            }
            if (root == null) return false;
            // if this is a single action, then we skip
            boolean skipMCTSMetrics = e.actions != null && e.actions.size() == 1;
            List<AbstractAction> actionsConsidered = root.actionsToConsider(root.actionsFromOpenLoopState);
            if (skipMCTSMetrics) {
                // to make sure, we also check the root stats, because there may have been actions added via a Decorator
                if (e.state.getGameTick() == root.state.getGameTick() && actionsConsidered.size() > 1 && actionsConsidered.contains(e.action))
                    skipMCTSMetrics = false;
            }
            if (!skipMCTSMetrics) {
                TreeStatistics treeStats = new TreeStatistics(root);
                int visits = root.getVisits();
                if (visits == 0) visits = 1;
                Map<AbstractAction, ActionStats> actionValueEstimates = root.actionValues;
                List<AbstractAction> sortedActions = actionValueEstimates.keySet().stream()
                        .filter(a -> actionValueEstimates.get(a) != null) // exclude those never tried (through pruning)
                        .filter(actionsConsidered::contains)  // exclude impossible actions (from reused parts of the tree)
                        .sorted(Comparator.comparingDouble(a -> -actionValueEstimates.get(a).valueOf(e.playerID)))
                        .toList(); // in descending order of value

                records.put("PlayerType", mctsPlayer.toString());
                records.put("PlayerID", e.state.getCurrentPlayer());
                records.put("Iterations", root.getVisits());
                records.put("ReusedVisits", mctsPlayer.root.inheritedVisits);
                records.put("MaxDepth", treeStats.depthReached);
                records.put("MeanLeafDepth", treeStats.meanLeafDepth);
                records.put("MeanNodeDepth", treeStats.meanNodeDepth);
                records.put("Nodes", treeStats.totalNodes);
                records.put("LeafNodes", treeStats.totalLeaves);
                records.put("OneActionNodes", treeStats.oneActionNodes);
                records.put("MeanActionsAtNode", treeStats.meanActionsAtNode);
                records.put("MeanActionsExpanded", treeStats.meanActionsExpanded);
                records.put("RolloutLength", mctsPlayer.root.rolloutActionsTaken / (double) visits);
                records.put("NodeClashes", mctsPlayer.root.nodeClash);
                OptionalInt maxVisits = Arrays.stream(root.actionVisits()).max();
                records.put("maxVisitProportion", (maxVisits.isPresent() ? maxVisits.getAsInt() : 0) / (double) visits);
                records.put("Action", e.action.getString(e.state));
                if (actionValueEstimates.containsKey(e.action)) {
                    // Action Taken ad Best Action may not be the same, either due to ROBUST selection, or because of DDA
                    records.put("ActionValue", actionValueEstimates.get(e.action).valueOf(e.playerID));
                    records.put("BestAction", sortedActions.get(0).getString(e.state));
                    records.put("BestActionValue", actionValueEstimates.get(sortedActions.get(0)).valueOf(e.playerID));
                    AbstractAction secondAction = sortedActions.get(1); // we know there are at least 2 actions
                    records.put("SecondAction", secondAction.getString(e.state));
                    records.put("SecondActionValue", actionValueEstimates.get(secondAction).valueOf(e.playerID));
                    // this may just be the same as the best action
                    AbstractAction worstAction = sortedActions.getLast();
                    records.put("WorstAction", worstAction.getString(e.state));
                    if (actionValueEstimates.containsKey(worstAction)) {
                        records.put("WorstActionValue", actionValueEstimates.get(worstAction).valueOf(e.playerID));
                    } else {
                   //     throw new AssertionError("WorstAction should have an estimate");
                        System.out.println("WorstAction should have an estimate");
                        throw new AssertionError("as above");
                    }
                } else {
//                    throw new AssertionError("Action should really have an estimate");
                    System.out.println("Warning: action has no value");
                    throw new AssertionError("as above");
                }
                records.put("ActionsAtRoot", root.actionValues.size());
                records.put("ActionsConsidered", actionsConsidered.size());
                records.put("fmCalls", mctsPlayer.root.fmCallsCount / visits);
                records.put("copyCalls", mctsPlayer.root.copyCount / visits);
                records.put("time", mctsPlayer.root.timeTaken);
                records.put("initTime", mctsPlayer.root.initialisationTimeTaken);
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.ACTION_CHOSEN));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> cols = new HashMap<>();
            cols.put("PlayerType", String.class);
            cols.put("PlayerID", Integer.class);
            cols.put("Iterations", Integer.class);
            cols.put("ReusedVisits", Integer.class); // visits from reused tree
            cols.put("MaxDepth", Integer.class);
            cols.put("MeanLeafDepth", Double.class);
            cols.put("MeanNodeDepth", Double.class);
            cols.put("Nodes", Integer.class);
            cols.put("LeafNodes", Integer.class);
            cols.put("OneActionNodes", Integer.class);
            cols.put("MeanActionsAtNode", Double.class);
            cols.put("MeanActionsExpanded", Double.class);
            cols.put("RolloutLength", Double.class);
            cols.put("NodeClashes", Integer.class);
            cols.put("maxVisitProportion", Double.class);
            cols.put("Action", String.class);
            cols.put("ActionValue", Double.class);
            cols.put("BestAction", String.class);
            cols.put("BestActionValue", Double.class);
            cols.put("SecondAction", String.class);
            cols.put("SecondActionValue", Double.class);
            cols.put("WorstAction", String.class);
            cols.put("WorstActionValue", Double.class);
            cols.put("ActionsAtRoot", Integer.class);
            cols.put("ActionsConsidered", Integer.class);
            cols.put("fmCalls", Integer.class);
            cols.put("copyCalls", Integer.class);
            cols.put("time", Double.class);
            cols.put("initTime", Double.class);
            return cols;
        }
    }


    public static class MultiTreeStats extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractPlayer player = listener.getGame().getPlayers().get(e.state.getCurrentPlayer());
            if (player instanceof MCTSPlayer mctsPlayer) {
                List<SingleTreeNode> otherRoots;
                if (mctsPlayer.root instanceof MultiTreeNode) {
                    otherRoots = Arrays.stream(((MultiTreeNode) mctsPlayer.root).roots).filter(Objects::nonNull)
                            .filter(node -> node.decisionPlayer != e.state.getCurrentPlayer())
                            .toList();
                    if (otherRoots.isEmpty())
                        return false; // can happen at end of game if other players have no moves
                } else {
                    return false;  // nothing to record
                }
                List<TreeStatistics> treeStats = otherRoots.stream().map(TreeStatistics::new).toList();
                records.put("PlayerType", mctsPlayer.toString());
                records.put("PlayerID", e.state.getCurrentPlayer());
                records.put("MaxDepth", treeStats.stream().mapToInt(ts -> ts.depthReached).average().orElse(0.0));
                records.put("MeanLeafDepth", treeStats.stream().mapToDouble(ts -> ts.meanLeafDepth).average().orElse(0.0));
                records.put("MeanNodeDepth", treeStats.stream().mapToDouble(ts -> ts.meanNodeDepth).average().orElse(0.0));
                records.put("Nodes", treeStats.stream().mapToInt(ts -> ts.totalNodes).average().orElse(0.0));
                records.put("LeafNodes", treeStats.stream().mapToInt(ts -> ts.totalLeaves).average().orElse(0.0));
                records.put("OneActionNodes", treeStats.stream().mapToInt(ts -> ts.oneActionNodes).average().orElse(0.0));
                records.put("MeanActionsAtNode", treeStats.stream().mapToDouble(ts -> ts.meanActionsAtNode).average().orElse(0.0));
                records.put("MeanActionsExpanded", treeStats.stream().mapToDouble(ts -> ts.meanActionsExpanded).average().orElse(0.0));
                records.put("ActionsAtRoot", otherRoots.stream().mapToInt(node -> node.actionValues.size()).average().orElse(0.0));
                records.put("ActionsConsidered", otherRoots.stream().mapToInt(node -> node.actionsToConsider(node.actionsFromOpenLoopState).size()).average().orElse(0.0));
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.ACTION_CHOSEN));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> cols = new HashMap<>();
            cols.put("PlayerType", String.class);
            cols.put("PlayerID", Integer.class);
            cols.put("MaxDepth", Double.class);
            cols.put("MeanLeafDepth", Double.class);
            cols.put("MeanNodeDepth", Double.class);
            cols.put("Nodes", Double.class);
            cols.put("LeafNodes", Double.class);
            cols.put("OneActionNodes", Double.class);
            cols.put("MeanActionsAtNode", Double.class);
            cols.put("MeanActionsExpanded", Double.class);
            cols.put("ActionsAtRoot", Double.class);
            cols.put("ActionsConsidered", Double.class);
            return cols;
        }
    }
}
