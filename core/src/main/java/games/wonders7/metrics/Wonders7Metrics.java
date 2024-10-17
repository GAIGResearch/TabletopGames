package games.wonders7.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;

import java.util.*;


public class Wonders7Metrics implements IMetricsCollection {

    public static class GameSeeds extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameState state = (Wonders7GameState) e.state;
            Wonders7GameParameters params = (Wonders7GameParameters) state.getGameParameters();
            records.put("CardSeed", params.cardShuffleSeed);
            records.put("WonderSeed", params.wonderShuffleSeed);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            columns.put("CardSeed", Integer.class);
            columns.put("WonderSeed", Integer.class);
            return columns;
        }
    }


    public static class Boards extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameState state = (Wonders7GameState) e.state;
            for (int i = 0; i < state.getNPlayers(); i++) {
                records.put("Player " + i +  " Board", state.getPlayerWonderBoard(i).type.name());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(Event.GameEvent.GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new LinkedHashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player " + i +  " Board", String.class);
            }
            return columns;
        }
    }


}