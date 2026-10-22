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


public class PIMCTSMetrics implements  IMetricsCollection {

    /**
     * Statistics of the search behind each decision by an MCTSPlayer. Forced moves (a single legal action) are not
     * searched, and are skipped.
     * <p>
     * With several determinisations (ForestNode) the statistics are computed on each tree: Nodes, LeafNodes,
     * OneActionNodes and the mean-type columns are means over the trees, MaxDepth and MaxActionsAtNode the maximum,
     * and Iterations, NodesTotal and the search counts are summed.
     */
    public static class TreeStats extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractPlayer player = listener.getGame().getPlayers().get(e.playerID);
            if (!(player instanceof MCTSPlayer mctsPlayer) || e.actions.size() <= 1)
                return false;
            // the nodes that hold the search counts, and the trees searched for this player's decision
            List<SingleTreeNode> searches;
            List<SingleTreeNode> trees;
            if (mctsPlayer.root instanceof ForestNode forest) {
                searches = forest.getRoots().stream().filter(Objects::nonNull).toList();
                trees = searches;
            } else if (mctsPlayer.root instanceof MultiTreeNode multiTree) {
                searches = List.of(mctsPlayer.root);
                trees = Arrays.stream(multiTree.roots).filter(Objects::nonNull)
                        .filter(node -> node.decisionPlayer == e.playerID)
                        .limit(1).toList();
            } else {
                searches = List.of(mctsPlayer.root);
                trees = searches;
            }
            if (trees.isEmpty()) return false;
            List<TreeStatistics> treeStats = trees.stream().map(TreeStatistics::new).toList();
            int iterations = trees.stream().mapToInt(SingleTreeNode::getVisits).sum();
            int visits = Math.max(iterations, 1);
            Map<AbstractAction, Integer> actionVisits = new HashMap<>();
            for (SingleTreeNode tree : trees)
                tree.getActionValues().forEach((action, stats) -> actionVisits.merge(action, stats.nVisits, Integer::sum));
            int maxVisits = actionVisits.values().stream().mapToInt(Integer::intValue).max().orElse(0);

            records.put("PlayerType", mctsPlayer.toString());
            records.put("PlayerID", e.playerID);
            records.put("Iterations", iterations);
            records.put("Trees", trees.size());
            records.put("ReusedVisits", searches.stream().mapToInt(n -> n.inheritedVisits).sum());
            records.put("MaxDepth", treeStats.stream().mapToInt(ts -> ts.depthReached).max().orElse(0));
            records.put("MeanLeafDepth", treeStats.stream().mapToDouble(ts -> ts.meanLeafDepth).average().orElse(0.0));
            records.put("MeanNodeDepth", treeStats.stream().mapToDouble(ts -> ts.meanNodeDepth).average().orElse(0.0));
            records.put("Nodes", treeStats.stream().mapToInt(ts -> ts.totalNodes).average().orElse(0.0));
            records.put("NodesTotal", treeStats.stream().mapToInt(ts -> ts.totalNodes).sum());
            records.put("LeafNodes", treeStats.stream().mapToInt(ts -> ts.totalLeaves).average().orElse(0.0));
            records.put("OneActionNodes", treeStats.stream().mapToInt(ts -> ts.oneActionNodes).average().orElse(0.0));
            records.put("MeanActionsAtNode", treeStats.stream().mapToDouble(ts -> ts.meanActionsAtNode).average().orElse(0.0));
            records.put("MaxActionsAtNode", treeStats.stream().mapToInt(ts -> ts.maxActionsAtNode).max().orElse(0));
            records.put("MeanActionsExpanded", treeStats.stream().mapToDouble(ts -> ts.meanActionsExpanded).average().orElse(0.0));
            records.put("RolloutLength", searches.stream().mapToInt(n -> n.rolloutActionsTaken).sum() / (double) visits);
            records.put("NodeClashes", searches.stream().mapToInt(n -> n.nodeClash).sum());
            records.put("maxVisitProportion", maxVisits / (double) visits);
            records.put("Action", e.action.getString(e.state));
            records.put("ActionsAtRoot", e.actions.size());
            records.put("fmCalls", searches.stream().mapToInt(n -> n.fmCallsCount).sum() / visits);
            records.put("copyCalls", searches.stream().mapToInt(n -> n.copyCount).sum() / visits);
            records.put("time", searches.stream().mapToDouble(n -> n.timeTaken).sum());
            records.put("initTime", mctsPlayer.root.initialisationTimeTaken);
            return true;
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
            cols.put("Trees", Integer.class);
            cols.put("ReusedVisits", Integer.class); // visits from reused tree
            cols.put("MaxDepth", Integer.class);
            cols.put("MeanLeafDepth", Double.class);
            cols.put("MeanNodeDepth", Double.class);
            cols.put("Nodes", Double.class);
            cols.put("NodesTotal", Integer.class);
            cols.put("LeafNodes", Double.class);
            cols.put("OneActionNodes", Double.class);
            cols.put("MeanActionsAtNode", Double.class);
            cols.put("MaxActionsAtNode", Integer.class);
            cols.put("MeanActionsExpanded", Double.class);
            cols.put("RolloutLength", Double.class);
            cols.put("NodeClashes", Integer.class);
            cols.put("maxVisitProportion", Double.class);
            cols.put("Action", String.class);
            cols.put("ActionsAtRoot", Integer.class);
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
            AbstractPlayer player = listener.getGame().getPlayers().get(e.playerID);
            if (player instanceof MCTSPlayer mctsPlayer) {
                List<SingleTreeNode> otherRoots;
                if (mctsPlayer.root instanceof MultiTreeNode) {
                    otherRoots = Arrays.stream(((MultiTreeNode) mctsPlayer.root).roots).filter(Objects::nonNull)
                            .filter(node -> node.decisionPlayer != e.playerID)
                            .toList();
                    if (otherRoots.isEmpty())
                        return false; // can happen at end of game if other players have no moves
                } else {
                    return false;  // nothing to record
                }
                List<TreeStatistics> treeStats = otherRoots.stream().map(TreeStatistics::new).toList();
                records.put("PlayerType", mctsPlayer.toString());
                records.put("PlayerID", e.playerID);
                records.put("MaxDepth", treeStats.stream().mapToInt(ts -> ts.depthReached).average().orElse(0.0));
                records.put("MeanLeafDepth", treeStats.stream().mapToDouble(ts -> ts.meanLeafDepth).average().orElse(0.0));
                records.put("MeanNodeDepth", treeStats.stream().mapToDouble(ts -> ts.meanNodeDepth).average().orElse(0.0));
                records.put("Nodes", treeStats.stream().mapToInt(ts -> ts.totalNodes).average().orElse(0.0));
                records.put("LeafNodes", treeStats.stream().mapToInt(ts -> ts.totalLeaves).average().orElse(0.0));
                records.put("OneActionNodes", treeStats.stream().mapToInt(ts -> ts.oneActionNodes).average().orElse(0.0));
                records.put("MeanActionsAtNode", treeStats.stream().mapToDouble(ts -> ts.meanActionsAtNode).average().orElse(0.0));
                records.put("MeanActionsExpanded", treeStats.stream().mapToDouble(ts -> ts.meanActionsExpanded).average().orElse(0.0));
                records.put("ActionsAtRoot", otherRoots.stream().mapToInt(node -> node.getActionValues().size()).average().orElse(0.0));
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
            return cols;
        }
    }
}
