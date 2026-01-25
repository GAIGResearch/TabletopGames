package games.cantstop.metrics;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.cantstop.CantStopGameState;
import games.cantstop.CantStopParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CantStopMetrics implements IMetricsCollection {

    public static class ChosenParams extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {

            CantStopGameState state = (CantStopGameState) e.state;
            CantStopParameters params = (CantStopParameters) state.getGameParameters();

            records.put("TWO_MAX", params.TWO_MAX);
            records.put("THREE_MAX", params.THREE_MAX);
            records.put("FOUR_MAX", params.FOUR_MAX);
            records.put("FIVE_MAX", params.FIVE_MAX);
            records.put("SIX_MAX", params.SIX_MAX);
            records.put("SEVEN_MAX", params.SEVEN_MAX);
            records.put("EIGHT_MAX", params.EIGHT_MAX);
            records.put("NINE_MAX", params.NINE_MAX);
            records.put("TEN_MAX", params.TEN_MAX);
            records.put("ELEVEN_MAX", params.ELEVEN_MAX);
            records.put("TWELVE_MAX", params.TWELVE_MAX);

            records.put("DICE_NUMBER", params.DICE_NUMBER);
            records.put("DICE_SIDES", params.DICE_SIDES);
            records.put("COLUMNS_TO_WIN", params.COLUMNS_TO_WIN);
            records.put("MARKERS", params.MARKERS);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("TWO_MAX", Integer.class);
            columns.put("THREE_MAX", Integer.class);
            columns.put("FOUR_MAX", Integer.class);
            columns.put("FIVE_MAX", Integer.class);
            columns.put("SIX_MAX", Integer.class);
            columns.put("SEVEN_MAX", Integer.class);
            columns.put("EIGHT_MAX", Integer.class);
            columns.put("NINE_MAX", Integer.class);
            columns.put("TEN_MAX", Integer.class);
            columns.put("ELEVEN_MAX", Integer.class);
            columns.put("TWELVE_MAX", Integer.class);

            columns.put("DICE_NUMBER", Integer.class);
            columns.put("DICE_SIDES", Integer.class);
            columns.put("COLUMNS_TO_WIN", Integer.class);
            columns.put("MARKERS", Integer.class);
            return columns;
        }
    }
}
