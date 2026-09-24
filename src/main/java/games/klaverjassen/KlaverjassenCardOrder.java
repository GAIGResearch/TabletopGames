package games.klaverjassen;

import core.components.FrenchCard;
import games.tricktaking.CardOrder;

/**
 * The order of the cards in a trick once trumps are chosen.
 *
 * @param trumps the trump suit, or null before trumps are chosen
 */
public record KlaverjassenCardOrder(FrenchCard.Suite trumps) implements CardOrder {

    @Override
    public FrenchCard.Suite suitOf(FrenchCard card) {
        return card.suite;
    }

    @Override
    public int rank(FrenchCard card) {
        // trumps rank J 9 A 10 K Q 8 7, other suits A 10 K Q J 9 8 7. FrenchCard numbers the Seven to Ten by value,
        // then Jack 11, Queen 12, King 13 and Ace 14
        if (card.suite == trumps) {
            return switch (card.number) {
                case 11 -> 8;  // Jack
                case 9 -> 7;
                case 14 -> 6;  // Ace
                case 10 -> 5;
                case 13 -> 4;  // King
                case 12 -> 3;  // Queen
                default -> card.number - 6;  // Eight 2, Seven 1
            };
        }
        return switch (card.number) {
            case 14 -> 8;  // Ace
            case 10 -> 7;
            case 13 -> 6;  // King
            case 12 -> 5;  // Queen
            case 11 -> 4;  // Jack
            default -> card.number - 6;  // Nine 3, Eight 2, Seven 1
        };
    }

    @Override
    public int hashCode() {
        // from the suit ordinal, as an enum's own hashCode differs between runs
        return trumps == null ? -1 : trumps.ordinal();
    }
}
