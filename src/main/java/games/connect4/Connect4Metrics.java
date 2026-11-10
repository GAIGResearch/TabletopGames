package games.connect4;

import core.AbstractGameState;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameMetrics;
import evaluation.metrics.IMetricsCollection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Connect4Metrics implements IMetricsCollection {

    public static class GameParams extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Connect4GameParameters params = (Connect4GameParameters) e.state.getGameParameters();
            // Upstream replaced the single square gridSize with an explicit width/height board
            records.put("width", params.width);
            records.put("height", params.height);
            records.put("winCount", params.winCount);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("width", Integer.class);
            columns.put("height", Integer.class);
            columns.put("winCount", Integer.class);
            return columns;
        }
    }

    public static class ColumnPieceCount extends AbstractMetric {

        /**
         * Number of column records declared by {@link #getColumns}. A board wider than this cannot be recorded:
         * the metric fails loudly rather than silently dropping the extra columns, because a record key without a
         * declared column is invisible in the resulting data.
         */
        static final int MAX_COLUMNS = 12;

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Connect4GameState gs = (Connect4GameState) e.state;
            if (gs.getWidth() > MAX_COLUMNS)
                throw new IllegalStateException("ColumnPieceCount declares " + MAX_COLUMNS
                        + " columns but this board has width " + gs.getWidth()
                        + "; raise MAX_COLUMNS so the declared columns cover the board.");
            for (int i = 0; i < MAX_COLUMNS; i++) {
                records.put(Integer.toString(i), 0);
            }
            for (int i = 0; i < gs.getWidth(); i++) {
                int count = 0;
                for (int j = 0; j < gs.getHeight(); j++) {
                    if (!gs.gridBoard.getElement(i, j).getComponentName().equals(Connect4Constants.emptyCell)) {
                        count++;
                    }
                }
                records.put(Integer.toString(i), count);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < MAX_COLUMNS; i++) {
                columns.put(Integer.toString(i), Integer.class);
            }
            return columns;
        }
    }

}
