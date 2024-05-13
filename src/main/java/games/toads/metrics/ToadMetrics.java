package games.toads.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.toads.ToadGameState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static evaluation.metrics.Event.GameEvent.GAME_OVER;
import static evaluation.metrics.Event.GameEvent.TURN_OVER;

public class ToadMetrics implements IMetricsCollection {

    public static class TurnMetrics extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            ToadGameState state = (ToadGameState) e.state;
            for (int i = 0; i < 2; i++) {
                records.put("P" + i + "_Battles", state.getBattlesWon(i, state.getRoundCounter()));
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Set.of(TURN_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            for (int i = 0; i < 2; i++) {
                columns.put("P" + i + "_Battles", Integer.class);
            }
            return columns;
        }
    }


    public static class GameMetrics extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            ToadGameState state = (ToadGameState) e.state;
            for (int i = 0; i < 2; i++) {
                for (int r = 0; r < 2; r++) {
                    records.put("P" + i + "_R" + r + "_Battles", state.getBattlesWon(i, r));
                }
                records.put("P" + i + "_TieBreaker", state.getTieBreaker(i).toString());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Set.of(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            for (int i = 0; i < 2; i++) {
                for (int r = 0; r < 2; r++) {
                    columns.put("P" + i + "_R" + r + "_Battles", Integer.class);
                }
                columns.put("P" + i + "_TieBreaker", String.class);
            }
            return columns;
        }
    }
}
