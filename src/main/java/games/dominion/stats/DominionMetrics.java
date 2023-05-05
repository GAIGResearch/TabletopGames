//package games.dominion.stats;
//import evaluation.listeners.MetricsGameListener;
//import evaluation.metrics.*;
//import games.dominion.DominionConstants;
//import games.dominion.DominionGameState;
//import games.dominion.cards.CardType;
//import utilities.Group;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//
//@SuppressWarnings("unused")
//public class DominionMetrics implements IMetricsCollection {
//
//    public static class VictoryCardsLeft extends AbstractParameterizedMetric {
//        public VictoryCardsLeft(){super();}
//        public VictoryCardsLeft(Object arg){super(arg);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            CardType type = (CardType) getParameterValue("type");
//            return ((DominionGameState)e.state).cardsOfType(type, -1, DominionConstants.DeckType.SUPPLY);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        public List<Group<String, List<?>, ?>> getAllowedParameters() {
//            return Collections.singletonList(new Group<>("type", Arrays.asList(CardType.values()), CardType.LIBRARY));
//        }
//    }
//
//    public static class EmptySupplySlots extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return ((DominionGameState)e.state).cardsIncludedInGame().stream()
//                    .filter(c -> ((DominionGameState)e.state).cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
//                    .count();
//        }
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//}
