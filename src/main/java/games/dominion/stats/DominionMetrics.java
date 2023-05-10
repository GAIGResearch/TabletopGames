package games.dominion.stats;

import core.Game;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.*;

@SuppressWarnings("unused")
public class DominionMetrics implements IMetricsCollection {

    public static class CardsInSupplyGameEnd extends AbstractMetric {
        CardType[] cardTypes;

        public CardsInSupplyGameEnd(){
            super();
            cardTypes = CardType.values();
        }
        public CardsInSupplyGameEnd(String[] args) {
            super(args);
            cardTypes = new CardType[args.length];
            for (int i = 0; i < args.length; i++) {
                cardTypes[i] = CardType.valueOf(args[i]);
            }
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (CardType type : cardTypes) {
                records.put(type.toString(), ((DominionGameState)e.state).cardsOfType(type, -1, DominionConstants.DeckType.SUPPLY));
            }
            return true;
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (CardType type : cardTypes) {
                columns.put(type.toString(), Integer.class);
            }
            return columns;
        }
    }

    public static class EmptySupplySlots extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("EmptySupplySlots", (int)((DominionGameState)e.state).cardsIncludedInGame().stream()
                    .filter(c -> ((DominionGameState)e.state).cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
                    .count());
            return true;
        }

        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            return Collections.singletonMap("EmptySupplySlots", Integer.class);
        }
    }
}
