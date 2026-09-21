package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.components.FrenchCard.FrenchCardType.*;

/**
 * Helpers shared by the tests of the trick-taking games (public so that games.whist and later the migrated games
 * can use them).
 */
public final class TrickTakingTestUtils {

    private TrickTakingTestUtils() {
    }

    /**
     * A card from a short code: rank (2-10, J, Q, K, A) then suit (H, D, C, S). e.g. "8H", "10D", "KS".
     */
    public static FrenchCard card(String code) {
        FrenchCard.Suite suit = switch (code.charAt(code.length() - 1)) {
            case 'H' -> FrenchCard.Suite.Hearts;
            case 'D' -> FrenchCard.Suite.Diamonds;
            case 'C' -> FrenchCard.Suite.Clubs;
            case 'S' -> FrenchCard.Suite.Spades;
            default -> throw new IllegalArgumentException("Unknown suit in " + code);
        };
        return switch (code.substring(0, code.length() - 1)) {
            case "J" -> new FrenchCard(Jack, suit);
            case "Q" -> new FrenchCard(Queen, suit);
            case "K" -> new FrenchCard(King, suit);
            case "A" -> new FrenchCard(Ace, suit);
            default -> new FrenchCard(Number, suit, Integer.parseInt(code.substring(0, code.length() - 1)));
        };
    }

    public static List<FrenchCard> cards(String... codes) {
        return Arrays.stream(codes).map(TrickTakingTestUtils::card).toList();
    }

    /**
     * A 4-player trick led by the given player holding these cards in play order (index 0 the lead). Built with
     * Deck.addToBottom, not Trick.play, so tests of winner etc. do not depend on play.
     */
    public static Trick trick(int leader, String... codes) {
        Trick t = new Trick("Trick", 4, leader);
        for (FrenchCard c : cards(codes))
            t.addToBottom(c);
        return t;
    }

    /**
     * The cards of a deck as a list, top (index 0) first - for comparing decks by content.
     */
    public static List<FrenchCard> cardsOf(Deck<FrenchCard> deck) {
        return new ArrayList<>(deck.getComponents());
    }
}
