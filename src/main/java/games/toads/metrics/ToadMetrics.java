package games.toads.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static evaluation.metrics.Event.GameEvent.ROUND_OVER;
import static evaluation.metrics.Event.GameEvent.TURN_OVER;

public class ToadMetrics implements IMetricsCollection {

    public static class TurnMetrics extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Set.of(TURN_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            for (int i = 0; i < 2; i++) {
                columns.put("Player " + i + " Battles", Integer.class);
            }
            return columns;
        }
    }
}
