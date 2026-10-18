package games.rummy;

import core.components.Deck;
import core.components.FrenchCard;
import games.rummy.actions.LayOff;

import java.util.Comparator;
import java.util.List;

public final class RummyUtils {

    // the order of the cards in a meld: by rank with Aces low, then by suit
    public static final Comparator<FrenchCard> MELD_ORDER =
            Comparator.comparingInt(RummyUtils::rank).thenComparing(c -> c.suite.ordinal());

    private RummyUtils() {
    }

    /**
     * @param meld the cards of a meld on the table, in MELD_ORDER
     * @return whether the card can be laid off onto the meld at the given position
     */
    public static boolean fits(List<FrenchCard> meld, FrenchCard card, LayOff.Position position) {
        // the cards of a run share a suit; those of a set do not
        boolean run = meld.get(0).suite == meld.get(1).suite;
        FrenchCard lowest = meld.get(0), highest = meld.get(meld.size() - 1);
        return switch (position) {
            case SET -> !run && rank(card) == rank(lowest);
            case LOW -> run && card.suite == lowest.suite && rank(card) == rank(lowest) - 1;
            case HIGH -> run && card.suite == highest.suite && rank(card) == rank(highest) + 1;
        };
    }

    /**
     * @return the card's rank, with Aces low (1)
     */
    public static int rank(FrenchCard card) {
        // FrenchCard numbers Aces as 14
        return card.type == FrenchCard.FrenchCardType.Ace ? 1 : card.number;
    }

    /**
     * @return the card's points in hand: Ace 1, court cards 10
     */
    public static int points(FrenchCard card) {
        return Math.min(rank(card), 10);
    }

    public static int points(Deck<FrenchCard> cards) {
        int total = 0;
        for (FrenchCard card : cards.getComponents())
            total += points(card);
        return total;
    }
}
