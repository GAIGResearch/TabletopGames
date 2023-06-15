package players.mcts;

import core.AbstractPlayer;
import core.Game;
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
            if (player instanceof MCTSPlayer) {
                MCTSPlayer mctsPlayer = (MCTSPlayer) player;
                SingleTreeNode root = mctsPlayer.root;
                if (root instanceof MultiTreeNode) {
                    root = Arrays.stream(((MultiTreeNode)root).roots).filter(Objects::nonNull)
                            .filter(node -> node.decisionPlayer == e.state.getCurrentPlayer())
                            .findFirst().orElseThrow(() -> new AssertionError("No root found for player " +e.state.getCurrentPlayer()));
                }
                TreeStatistics treeStats = new TreeStatistics(root);
                records.put("PlayerType", mctsPlayer.toString());
                records.put("Iterations", mctsPlayer.root.getVisits());
                records.put("MaxDepth", treeStats.depthReached);
                records.put("MeanLeafDepth", treeStats.meanLeafDepth);
                records.put("Nodes", treeStats.totalNodes);
                records.put("OneActionNodes", treeStats.oneActionNodes);
                records.put("MeanActionsAtNode", treeStats.meanActionsAtNode);
                records.put("RolloutLength", mctsPlayer.root.rolloutActionsTaken / (double) mctsPlayer.root.getVisits());
                OptionalInt maxVisits = Arrays.stream(root.actionVisits()).max();
                records.put("maxVisitProportion", (maxVisits.isPresent() ? maxVisits.getAsInt() : 0) / (double) root.getVisits());
                records.put("Action", e.action.getString(e.state));
                records.put("ActionsAtRoot", root.children.size());
                records.put("fmCalls", mctsPlayer.root.fmCallsCount / mctsPlayer.root.getVisits());
                records.put("copyCalls", mctsPlayer.root.copyCount / mctsPlayer.root.getVisits());
                return true;
            }
            return false;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.ACTION_CHOSEN));
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> cols = new HashMap<>();
            cols.put("PlayerType", String.class);
            cols.put("Iterations", Integer.class);
            cols.put("MaxDepth", Integer.class);
            cols.put("MeanLeafDepth", Double.class);
            cols.put("Nodes", Integer.class);
            cols.put("OneActionNodes", Integer.class);
            cols.put("MeanActionsAtNode", Double.class);
            cols.put("RolloutLength", Double.class);
            cols.put("maxVisitProportion", Double.class);
            cols.put("Action", String.class);
            cols.put("ActionsAtRoot", Integer.class);
            cols.put("fmCalls", Integer.class);
            cols.put("copyCalls", Integer.class);
            return cols;
        }
    }


    public static class MultiTreeStats extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractPlayer player = listener.getGame().getPlayers().get(e.state.getCurrentPlayer());
            if (player instanceof MCTSPlayer) {
                MCTSPlayer mctsPlayer = (MCTSPlayer) player;
                List<SingleTreeNode> otherRoots;
                if (mctsPlayer.root instanceof MultiTreeNode) {
                    otherRoots = Arrays.stream(((MultiTreeNode)mctsPlayer.root).roots).filter(Objects::nonNull)
                            .filter(node -> node.decisionPlayer != e.state.getCurrentPlayer())
                            .collect(Collectors.toList());
                    if (otherRoots.isEmpty())
                        return false; // can happen at end of game if other players have no moves
                } else {
                    return false;  // nothing to record
                }
                List<TreeStatistics> treeStats = otherRoots.stream().map(TreeStatistics::new).collect(Collectors.toList());
                records.put("PlayerType", mctsPlayer.toString());
                records.put("MaxDepth", treeStats.stream().mapToInt(ts -> ts.depthReached).average().orElse(0.0));
                records.put("MeanLeafDepth", treeStats.stream().mapToDouble(ts -> ts.meanLeafDepth).average().orElse(0.0));
                records.put("Nodes", treeStats.stream().mapToInt(ts -> ts.totalNodes).average().orElse(0.0));
                records.put("OneActionNodes", treeStats.stream().mapToInt(ts -> ts.oneActionNodes).average().orElse(0.0));
                records.put("MeanActionsAtNode", treeStats.stream().mapToDouble(ts -> ts.meanActionsAtNode).average().orElse(0.0));
                records.put("RolloutLength", otherRoots.stream().mapToInt(root -> root.rolloutActionsTaken).average().orElse(0.0) / mctsPlayer.root.getVisits());
                records.put("ActionsAtRoot", otherRoots.stream().mapToInt(node -> node.children.size()).average().orElse(0.0));
                return true;
            }
            return false;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.ACTION_CHOSEN));
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> cols = new HashMap<>();
            cols.put("PlayerType", String.class);
            cols.put("MaxDepth", Double.class);
            cols.put("MeanLeafDepth", Double.class);
            cols.put("Nodes", Double.class);
            cols.put("OneActionNodes", Double.class);
            cols.put("MeanActionsAtNode", Double.class);
            cols.put("RolloutLength", Double.class);
            cols.put("ActionsAtRoot", Double.class);
            return cols;
        }
    }
}
