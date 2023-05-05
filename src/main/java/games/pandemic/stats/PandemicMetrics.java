//package games.pandemic.stats;
//import core.components.Card;
//import core.components.Component;
//import core.components.Counter;
//import core.components.Deck;
//import evaluation.listeners.MetricsGameListener;
//import evaluation.metrics.*;
//import games.pandemic.PandemicGameState;
//import utilities.Group;
//import utilities.Hash;
//
//import java.util.*;
//
//@SuppressWarnings("unused")
//public class PandemicMetrics implements IMetricsCollection {
//
//    public static class DeckSize extends AbstractParameterizedMetric {
//        public DeckSize(){super();}
//        public DeckSize(Object arg){super(arg);}
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
//        }
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
//            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash((String) getParameterValue(("deckName"))));
//            //if(c == null) throw new AssertionError("name '" + key + "' does not correspond to any component in this game.");
//            return ((Deck<Card>) c).getSize();
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("deckName", Arrays.asList("infection", "Player Deck"), "infection"));
//        }
//    }
//
//    public static class CounterValue extends AbstractParameterizedMetric {
//        public CounterValue(){super();}
//        public CounterValue(Object arg){super(arg);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            String counterName = (String) getParameterValue("counterName");
//            //if(key == null) throw new AssertionError("Argument for Constructor in " + getClass().getName() + " can't be null");
//            Component c = ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(counterName));
//            if(c == null) {
//                ((PandemicGameState)e.state).getComponent(Hash.GetInstance().hash(counterName));
//                throw new AssertionError("name '" + counterName + "' does not correspond to any component in this game.");
//            }
//                return ((Counter) c).getValue();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("counterName", new ArrayList<>(Arrays.asList("Disease yellow", "Disease red", "Disease blue", "Disease black",
//                    "Disease Cube yellow", "Disease Cube red", "Disease Cube blue", "Disease Cube black",
//                    "Outbreaks", "Infection Rate", "Research Stations")), "Research Stations"));
//        }
//    }
//
//
//}
