package games.schwimmen;

import core.components.FrenchCard;

import java.util.Arrays;
import java.util.List;

public class SchwimmenUtils {

    private SchwimmenUtils() {
    }

    public static double handValue(List<FrenchCard> hand, SchwimmenParameters params) {
        if (isFeuer(hand))
            return params.threeAcesValue;
        if (isThreeOfAKind(hand))
            return params.threeOfAKindValue;
        return suitTotal(hand, bestSuit(hand, params), params);
    }

    /**
     * True when all the cards (three or more) have the same rank.
     */
    public static boolean isThreeOfAKind(List<FrenchCard> hand) {
        return hand.size() >= 3 && hand.stream().allMatch(c -> c.type == hand.get(0).type && c.number == hand.get(0).number);
    }

    /**
     * The suit with the highest total in the hand; of two with the same total, the higher-ranking suit.
     */
    static FrenchCard.Suite bestSuit(List<FrenchCard> hand, SchwimmenParameters params) {
        FrenchCard.Suite best = null;
        for (FrenchCard.Suite suit : SchwimmenParameters.SUIT_ORDER) {
            // the suits run from lowest to highest, so an equal total in a later suit replaces an earlier one
            if (best == null || suitTotal(hand, suit, params) >= suitTotal(hand, best, params))
                best = suit;
        }
        return best;
    }

    static int suitTotal(List<FrenchCard> hand, FrenchCard.Suite suit, SchwimmenParameters params) {
        int total = 0;
        for (FrenchCard card : hand)
            if (card.suite == suit)
                total += params.cardValue(card);
        return total;
    }

    /**
     * True for Schnauz: a single-suit total of SchwimmenParameters.schnauzTotal.
     */
    public static boolean isSchnauz(List<FrenchCard> hand, SchwimmenParameters params) {
        for (FrenchCard.Suite suit : FrenchCard.Suite.values())
            if (suitTotal(hand, suit, params) == params.schnauzTotal)
                return true;
        return false;
    }

    /**
     * True for Feuer: three Aces.
     */
    public static boolean isFeuer(List<FrenchCard> hand) {
        return hand.stream().filter(c -> c.type == FrenchCard.FrenchCardType.Ace).count() >= 3;
    }

    /**
     * Orders hands of equal value: the higher the tiebreak, the better the hand.
     */
    public static double tiebreak(List<FrenchCard> hand, SchwimmenParameters params) {
        // three of a kind by rank (FrenchCard numbers the Ace 14), above any suit should the values ever be equal
        if (isThreeOfAKind(hand))
            return 100 + hand.get(0).number;
        // otherwise the rank of the suit of the best total
        return SchwimmenParameters.SUIT_ORDER.indexOf(bestSuit(hand, params));
    }

    /**
     * A visibility array for a card every player has seen.
     */
    public static boolean[] visibleToAll(int nPlayers) {
        boolean[] visibility = new boolean[nPlayers];
        Arrays.fill(visibility, true);
        return visibility;
    }
}
