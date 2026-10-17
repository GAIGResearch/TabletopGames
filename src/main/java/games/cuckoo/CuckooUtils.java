package games.cuckoo;

import core.components.FrenchCard;

/**
 * Card ranking for Cuckoo: King high, Ace low, suits irrelevant.
 */
public final class CuckooUtils {

    private CuckooUtils() {
    }

    /**
     * The rank of the card, from 1 (Ace) to 13 (King).
     */
    public static int rank(FrenchCard card) {
        // FrenchCard numbers Aces 14, but Aces are low in Cuckoo
        return card.type == FrenchCard.FrenchCardType.Ace ? 1 : card.number;
    }

    public static boolean isKing(FrenchCard card) {
        return card.type == FrenchCard.FrenchCardType.King;
    }
}
