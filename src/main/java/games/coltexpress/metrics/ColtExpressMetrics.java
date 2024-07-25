package games.coltexpress.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.*;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;

import java.util.*;

public class ColtExpressMetrics implements IMetricsCollection {

    public static class GameSeeds extends AbstractMetric {

        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("TrainSeed", Integer.class);
            columns.put("RoundSeed", Integer.class);
            columns.put("CharacterSeed", Integer.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            ColtExpressGameState state = (ColtExpressGameState) e.state;
            ColtExpressParameters params = (ColtExpressParameters) state.getGameParameters();
            records.put("TrainSeed", params.trainShuffleSeed);
            records.put("RoundSeed", params.roundDeckShuffleSeed);
            records.put("CharacterSeed", params.initialCharacterShuffleSeed);
            return true;
        }
    }
}
