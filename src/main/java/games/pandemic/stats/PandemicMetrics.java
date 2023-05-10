package games.pandemic.stats;
import core.AbstractGameState;
import core.Game;
import core.components.BoardNode;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import core.properties.PropertyIntArray;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.*;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import games.pandemic.PandemicParameters;
import utilities.Group;
import utilities.Hash;

import java.util.*;

import static core.CoreConstants.GameResult.WIN_GAME;
import static games.pandemic.PandemicConstants.colors;
import static games.pandemic.PandemicConstants.infectionHash;

@SuppressWarnings("unused")
public class PandemicMetrics implements IMetricsCollection {

    public static class DeckSize extends AbstractParameterizedMetric {
        public DeckSize(){super();}

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash((String) getParameterValue(("deckName"))));
            //if(c == null) throw new AssertionError("name '" + key + "' does not correspond to any component in this game.");
            records.put("Size", ((Deck<?>) c).getSize());
            return true;
        }

        public DeckSize(Object arg){super(arg);}
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Size", Integer.class);
            return columns;
        }

        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("deckName", Arrays.asList("infection", "Player Deck"), "infection"));
        }
    }

    public static class CounterValue extends AbstractParameterizedMetric {
        public CounterValue(){super();}

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            String counterName = (String) getParameterValue("counterName");
            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(counterName));
            if(c == null) {
                ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(counterName));
                throw new AssertionError("name '" + counterName + "' does not correspond to any component in this game.");
            }
            records.put("Value", ((Counter) c).getValue());
            return true;
        }

        public CounterValue(Object arg){super(arg);}


        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Value", Integer.class);
            return columns;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        public List<Group<String, List<?>, ?>> getAllowedParameters() {
            return Collections.singletonList(new Group<>("counterName", new ArrayList<>(Arrays.asList("Disease yellow", "Disease red", "Disease blue", "Disease black",
                    "Disease Cube yellow", "Disease Cube red", "Disease Cube blue", "Disease Cube black",
                    "Outbreaks", "Infection Rate", "Research Stations")), "Research Stations"));
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

        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        @Override
        public Map<String, Class<?>> getColumns(Game game) {
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
