package games.pandemic.stats;

import core.AbstractGameState;
import core.components.BoardNode;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IGameEvent;
import core.properties.PropertyIntArray;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import utilities.Hash;

import java.util.*;

import static core.CoreConstants.GameResult.WIN_GAME;
import static games.pandemic.PandemicConstants.colors;
import static games.pandemic.PandemicConstants.infectionHash;

@SuppressWarnings("unused")
public class PandemicMetrics implements IMetricsCollection {

    public static class DeckSize extends AbstractMetric {
        String[] deckNames = new String[]{"infection", "Player Deck"};

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (String deck: deckNames) {
                Component c = ((PandemicGameState) e.state).getComponent(Hash.GetInstance().hash(deck));
                records.put(deck + " Deck Size", ((Deck<?>) c).getSize());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String deck: deckNames) {
                columns.put(deck + " Deck Size", Integer.class);
            }
            return columns;
        }
    }

    public static class CounterValue extends AbstractMetric {
        String[] counters = new String[] {
                "Disease yellow", "Disease red", "Disease blue", "Disease black",
                "Disease Cube yellow", "Disease Cube red", "Disease Cube blue", "Disease Cube black",
                "Outbreaks", "Infection Rate", "Research Stations"
        };

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (String counter: counters) {
                Component c = ((PandemicGameState) e.state).getComponent(Hash.GetInstance().hash(counter));
                records.put(counter, ((Counter) c).getValue());
            }
            return true;
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String counter: counters) {
                columns.put(counter, Integer.class);
            }
            return columns;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }


    public static class EndGameStats extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("Victory", e.state.getGameStatus()==WIN_GAME? 1.0 : 0.0);
            records.put("# Cured Diseases", countDisease(e.state, 1, false)+countDisease(e.state, 2, false));
            records.put("# Outbreaks", ((Counter) ((PandemicGameState)e.state).getComponent(PandemicConstants.outbreaksHash)).getValue());
            records.put("# Endangered Cities", countCityDanger(listener, e));
            records.put("# Disease Cubes Left", countDisease(e.state, 0, true));
            records.put("# Eradicated Diseases", countDisease(e.state, 2, false));
            return true;
        }

        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Victory", Double.class);
            columns.put("# Cured Diseases", Integer.class);
            columns.put("# Outbreaks", Integer.class);
            columns.put("# Endangered Cities", Integer.class);
            columns.put("# Disease Cubes Left", Integer.class);
            columns.put("# Eradicated Diseases", Integer.class);
            return columns;
        }
    }


    static int countDisease(AbstractGameState state, int targetValue, boolean cubes) {
        PandemicGameState pgs = (PandemicGameState) state;
        int count = 0;
        for (String color: colors) {
            if (cubes) {
                count += ((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color))).getValue();
            } else {
                if (((Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color))).getValue() == targetValue)
                    count++;
            }
        }
        return count;
    }

    static int countCityDanger(MetricsGameListener listener, Event event) {
        PandemicGameState pgs = (PandemicGameState) event.state;
        PandemicParameters pp = (PandemicParameters) pgs.getGameParameters();
        int count = 0;

        for (BoardNode bn: pgs.getWorld().getBoardNodes()) {
            PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
            int[] array = infectionArray.getValues();
            for (int a: array) {
                if (a >= pp.getMaxCubesPerCity() -1) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

}
