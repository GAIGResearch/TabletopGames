package games.sueca;

import core.CoreConstants;
import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.CardOrder;

public final class SuecaUtils {

    private SuecaUtils() {
    }

    /**
     * The order of the cards in a trick, which is the same in every suit, trumps included.
     */
    public static final CardOrder<FrenchCard, FrenchCard.Suite> CARD_ORDER = new CardOrder<>() {
        @Override
        public FrenchCard.Suite suitOf(FrenchCard card) {
            return card.suite;
        }

        @Override
        public int rank(FrenchCard card) {
            // A 7 K J Q 6 5 4 3 2. FrenchCard numbers Two to Seven by value, then Jack 11, Queen 12, King 13 and
            // Ace 14
            return switch (card.number) {
                case 14 -> 10;  // Ace
                case 7 -> 9;
                case 13 -> 8;   // King
                case 11 -> 7;   // Jack
                case 12 -> 6;   // Queen
                default -> card.number - 1;  // Six 5 ... Two 1
            };
        }

        @Override
        public String toString() {
            return "SUECA";
        }
    };

    /**
     * The games a team scores in the rubber for a deal it won with more than 60 card points, before any extra games
     * from tied deals.
     */
    public static int gamesForDeal(int cardPoints, boolean allTricks) {
        // every trick is needed for 4 games: 120 card points with a trick lost score only 2
        if (allTricks)
            return 4;
        return cardPoints >= 91 ? 2 : 1;
    }

    /**
     * The 40-card Sueca pack.
     */
    public static Deck<FrenchCard> newPack(String name, CoreConstants.VisibilityMode visibility) {
        // a standard pack without the Eights, Nines and Tens
        Deck<FrenchCard> pack = FrenchCard.generateDeck(name, visibility);
        pack.getComponents().removeIf(c -> c.number >= 8 && c.number <= 10);
        return pack;
    }
}
