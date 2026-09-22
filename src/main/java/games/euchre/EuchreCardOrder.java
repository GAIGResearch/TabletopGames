package games.euchre;

import core.components.FrenchCard;
import games.tricktaking.CardOrder;

/**
 * The order of the cards once trumps are chosen: the Jack of trumps (the right bower) is the highest trump, and the
 * other Jack of the same colour (the left bower) belongs to the trump suit and is the next highest. All other cards
 * belong to their printed suit and rank by number, Aces high.
 */
public record EuchreCardOrder(FrenchCard.Suite trumps) implements CardOrder {

    @Override
    public FrenchCard.Suite suitOf(FrenchCard card) {
        return isLeftBower(card) ? trumps : card.suite;
    }

    @Override
    public int rank(FrenchCard card) {
        // FrenchCard numbers the Ace 14, so 16 and 15 put the bowers above it
        if (card.type == FrenchCard.FrenchCardType.Jack && card.suite == trumps)
            return 16;
        return isLeftBower(card) ? 15 : card.number;
    }

    private boolean isLeftBower(FrenchCard card) {
        return card.type == FrenchCard.FrenchCardType.Jack && card.suite != trumps
                && isRed(card.suite) == isRed(trumps);
    }

    private static boolean isRed(FrenchCard.Suite suit) {
        return suit == FrenchCard.Suite.Hearts || suit == FrenchCard.Suite.Diamonds;
    }
}
