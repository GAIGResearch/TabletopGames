package games.wonders7.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;

import java.util.*;


public class GameAttributes implements IMetricsCollection {

    public static class GameSeeds extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Wonders7GameState state = (Wonders7GameState) e.state;
            Wonders7GameParameters params = (Wonders7GameParameters) state.getGameParameters();
            records.put("CardSeed", params.cardShuffleSeed);
            records.put("WonderSeed", params.wonderDistributionSeed);
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

}