package games.tricktaking;

import core.components.FrenchCard;

/**
 * How a game orders the cards in a trick: the suit each card belongs to (for leading, following, trumping and known
 * voids), and its rank within that suit. Most games use {@link #STANDARD}; Euchre's left bower belongs to the trump
 * suit, not to its printed suit.
 */
public interface CardOrder {

    /**
     * The suit the card belongs to.
     */
    FrenchCard.Suite suitOf(FrenchCard card);

    /**
     * The card's rank within its suit: a higher rank beats a lower one.
     */
    int rank(FrenchCard card);

    /**
     * Each card belongs to its printed suit, and ranks by its number (Aces high).
     */
    CardOrder STANDARD = new CardOrder() {
        @Override
        public FrenchCard.Suite suitOf(FrenchCard card) {
            return card.suite;
        }

        @Override
        public int rank(FrenchCard card) {
            return card.number;
        }

        @Override
        public String toString() {
            return "STANDARD";
        }
    };
}
