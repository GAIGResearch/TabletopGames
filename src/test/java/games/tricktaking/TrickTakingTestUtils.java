package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;
import core.components.TarotCard;

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
    public static Trick<FrenchCard, FrenchCard.Suite> trick(int leader, String... codes) {
        Trick<FrenchCard, FrenchCard.Suite> t = new Trick<>("Trick", 4, leader, CardOrder.STANDARD);
        for (FrenchCard c : cards(codes))
            t.addToBottom(c);
        return t;
    }

    /**
     * A tarot card order for testing suitless cards without depending on any tarot game. Not Scarto's order, whose
     * round suits rank differently.
     */
    public static final CardOrder<TarotCard, TarotCard.Suit> TAROT_ORDER = new CardOrder<>() {
        @Override
        public TarotCard.Suit suitOf(TarotCard card) {
            return card.isFool() ? null : card.suit;
        }

        @Override
        public int rank(TarotCard card) {
            return card.number;
        }

        @Override
        public String toString() {
            return "TAROT_ORDER";
        }
    };

    public static TarotCard cup(int n) {
        return new TarotCard(TarotCard.Suit.Cups, n);
    }

    public static TarotCard coin(int n) {
        return new TarotCard(TarotCard.Suit.Coins, n);
    }

    public static TarotCard sword(int n) {
        return new TarotCard(TarotCard.Suit.Swords, n);
    }

    public static TarotCard trump(int n) {
        return new TarotCard(TarotCard.Suit.Trumps, n);
    }

    /**
     * A new Fool (equal to TarotCard.FOOL).
     */
    public static TarotCard fool() {
        return new TarotCard(TarotCard.Suit.None, 0);
    }

    /**
     * A 3-player tarot trick with {@link #TAROT_ORDER}, led by the given player, holding these cards in play order
     * (index 0 the lead). Built with Deck.addToBottom, not Trick.play.
     */
    public static Trick<TarotCard, TarotCard.Suit> tarotTrick(int leader, TarotCard... cards) {
        Trick<TarotCard, TarotCard.Suit> t = new Trick<>("Trick", 3, leader, TAROT_ORDER);
        for (TarotCard c : cards)
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
