package players.mcts;

import core.AbstractPlayer;
import core.Game;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;

import java.util.*;

public class MCTSMetrics implements IMetricsCollection {


    public static class Base extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractPlayer player = listener.getGame().getPlayers().get(e.state.getCurrentPlayer());
            if (player instanceof MCTSPlayer) {
                MCTSPlayer mctsPlayer = (MCTSPlayer) player;
                TreeStatistics treeStats = new TreeStatistics(mctsPlayer.root);
                records.put("PlayerType", mctsPlayer.toString());
                records.put("Iterations", mctsPlayer.root.getVisits());
                records.put("Depth", treeStats.depthReached);
                records.put("Nodes", treeStats.totalNodes);
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
            cols.put("Depth", Integer.class);
            cols.put("Nodes", Integer.class);
            return cols;
        }
    }
}
