package games.president;

import core.components.FrenchCard;

public class PresidentUtils {

    /**
     * @return the rank of the card in play order
     */
    public static int rank(FrenchCard card) {
        return rank(card.number);
    }

    /**
     * @return the rank of the FrenchCard number (2..14) in play order
     */
    public static int rank(int number) {
        // FrenchCard numbers the Ace 14; the Two ranks above it
        return number == 2 ? 15 : number;
    }

    /**
     * @return the name of the rank with the FrenchCard number (2..14)
     */
    public static String rankName(int number) {
        return switch (number) {
            case 11 -> FrenchCard.FrenchCardType.Jack.name();
            case 12 -> FrenchCard.FrenchCardType.Queen.name();
            case 13 -> FrenchCard.FrenchCardType.King.name();
            case 14 -> FrenchCard.FrenchCardType.Ace.name();
            default -> String.valueOf(number);
        };
    }
}
