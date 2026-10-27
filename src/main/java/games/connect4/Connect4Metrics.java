package games.connect4;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
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

    public static class SaveStateConnect4 extends GameMetrics.SaveStateOnEvent {

        public SaveStateConnect4(String[] args) {
            super(args);
        }

        @Override
        protected AbstractGameStateContainer getGSContainer(AbstractGameState gs) {
            return new Connect4GameStateContainer((Connect4GameState) gs);
        }

        @Override
        protected Set<IGameEvent> getSaveEventTypes() {
            return Collections.singleton(Event.GameEvent.TURN_OVER);
        }
    }

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
        int maxColumns = 12;
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Connect4GameState gs = (Connect4GameState) e.state;
            for (int i = 0; i < maxColumns; i++) {
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
            for (int i = 0; i < maxColumns; i++) {
                columns.put(Integer.toString(i), Integer.class);
            }
            return columns;
        }
    }

}
