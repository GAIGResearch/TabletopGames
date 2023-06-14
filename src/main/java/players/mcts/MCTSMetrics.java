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
                // TODO: Currently this only produces the statistics for the deciding player in MT-MCTS
                // This can be straightforwardly be extended to produce statistics for all players (or their mean) from the other roots
                TreeStatistics treeStats = new TreeStatistics(root);
                records.put("PlayerType", mctsPlayer.toString());
                records.put("Iterations", root.getVisits());
                records.put("MaxDepth", treeStats.depthReached);
                records.put("MeanLeafDepth", treeStats.meanLeafDepth);
                records.put("Nodes", treeStats.totalNodes);
                records.put("OneActionNodes", treeStats.oneActionNodes);
                records.put("MeanActionsAtNode", treeStats.meanActionsAtNode);
                records.put("RolloutLength", root.rolloutActionsTaken / (double) root.getVisits());
                OptionalInt maxVisits = Arrays.stream(root.actionVisits()).max();
                records.put("maxVisitProportion", (maxVisits.isPresent() ? maxVisits.getAsInt() : 0) / (double) root.getVisits());
                records.put("Action", e.action.getString(e.state));
                records.put("ActionsAtRoot", root.children.size());
                records.put("fmCalls", root.fmCallsCount / root.getVisits());
                records.put("copyCalls", root.copyCount / root.getVisits());
            }
            return true;
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
}
