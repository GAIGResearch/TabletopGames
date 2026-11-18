package games.cantstop;

import core.AbstractGameState;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameMetrics;
import evaluation.metrics.IMetricsCollection;
import games.cantstop.actions.AllocateDice;
import games.cantstop.actions.Pass;
import games.cantstop.actions.RollDice;

import java.util.*;

public class CantStopMetrics implements IMetricsCollection {

    public static class GameParams extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            CantStopParameters params = (CantStopParameters) e.state.getGameParameters();
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
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>>  columns = new HashMap<>();
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
            return columns;
        }
    }

    public static class ScoresPerTurn extends AbstractMetric {

        ArrayList<Double> scoreArrayPerTurn = new ArrayList<>();
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == Event.GameEvent.TURN_OVER && e.state.getTurnCounter() % e.state.getNPlayers() == 0) {
                for (int i=0; i<e.state.getNPlayers(); i++) {
                    scoreArrayPerTurn.add(e.state.getGameScore(i));
                }
                return false;
            }
            if (e.type == Event.GameEvent.GAME_OVER) {
                for (int i=0; i<e.state.getNPlayers(); i++) {
                    scoreArrayPerTurn.add(e.state.getGameScore(i));
                }
                String scoreArrayStr = scoreArrayPerTurn.toString();
                records.put("ScoresPerTurn", scoreArrayStr);
                return true;
            }
            return false;
        }

        @Override
        public void notifyGameOver() {
            super.notifyGameOver();
            scoreArrayPerTurn.clear();
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Set.of(Event.GameEvent.TURN_OVER, Event.GameEvent.GAME_OVER);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("ScoresPerTurn", String.class);
            return columns;
        }
    }

    public static class ActionsCount extends GameMetrics.ActionsCount {

        @Override
        protected void initializeActionsTracked() {
            actionsTracked = Set.of(AllocateDice.class, Pass.class, RollDice.class);
        }
    }
}
