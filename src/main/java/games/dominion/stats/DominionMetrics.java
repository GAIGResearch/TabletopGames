package games.dominion.stats;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;
public class DominionMetrics {


    public static class VictoryCardsLeft extends AbstractMetric {

        private CardType type;
        public VictoryCardsLeft(String type) {
            addEventType(Event.GameEvent.GAME_OVER);
            this.type = CardType.valueOf(type);
        }

        public String name() {return "VictoryCardsLeft (" + type + ")";}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DominionGameState)e.state).cardsOfType(type, -1, DominionConstants.DeckType.SUPPLY);
        }
    }

    public static class EmptySupplySlots extends AbstractMetric {

        public EmptySupplySlots() {addEventType(Event.GameEvent.GAME_OVER);}

        @Override
        public Object run(GameListener listener, Event e) {
            return ((DominionGameState)e.state).cardsIncludedInGame().stream()
                    .filter(c -> ((DominionGameState)e.state).cardsOfType(c, -1, DominionConstants.DeckType.SUPPLY) == 0)
                    .count();
        }
    }
}
