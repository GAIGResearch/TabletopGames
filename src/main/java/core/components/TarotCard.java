package core.components;

import core.CoreConstants;

import java.util.Objects;

/**
 * <p>A card of the 78-card Italian-suited tarot pack (e.g. the Tarocco Piemontese): four suits of fourteen cards,
 * 21 trumps, and the Fool.</p>
 *
 * <ul>
 *     <li>Suit cards are numbered 1 (Ace) to 10, then Knave 11, Cavalier 12, Queen 13, King 14.</li>
 *     <li>Trumps are numbered 1 (the Pagat) to 21 (the World); trump 20 is the Angel.</li>
 *     <li>The Fool has the suit {@link Suit#None} and number 0.</li>
 * </ul>
 *
 * <p>How the cards rank is up to each game.</p>
 */
public class TarotCard extends Card {

    /**
     * Swords and Batons are the long suits, Cups and Coins the round suits.
     */
    public enum Suit {
        Swords,
        Batons,
        Cups,
        Coins,
        Trumps,
        None
    }

    public static final int KNAVE = 11, CAVALIER = 12, QUEEN = 13, KING = 14;
    public static final int PAGAT = 1, ANGEL = 20, WORLD = 21;
    public static final int N_TRUMPS = 21;

    // for comparisons: every card of the same suit and number is equal
    public static final TarotCard FOOL = new TarotCard(Suit.None, 0);

    public final Suit suit;
    public final int number;

    public TarotCard(Suit suit, int number) {
        super(nameOf(suit, number));
        this.suit = suit;
        this.number = number;
    }

    /**
     * All 78 cards: each suit from Ace to King, then the trumps from 1 to 21, then the Fool.
     */
    public static Deck<TarotCard> generateDeck(String name, CoreConstants.VisibilityMode visibilityMode) {
        Deck<TarotCard> deck = new Deck<>(name, visibilityMode);
        for (Suit suit : new Suit[]{Suit.Swords, Suit.Batons, Suit.Cups, Suit.Coins})
            for (int n = 1; n <= KING; n++)
                deck.addToBottom(new TarotCard(suit, n));
        for (int n = 1; n <= N_TRUMPS; n++)
            deck.addToBottom(new TarotCard(Suit.Trumps, n));
        deck.addToBottom(new TarotCard(Suit.None, 0));
        return deck;
    }

    public boolean isFool() {
        return suit == Suit.None;
    }

    public boolean isTrump() {
        return suit == Suit.Trumps;
    }

    private static String nameOf(Suit suit, int number) {
        return switch (suit) {
            case None -> "Fool";
            case Trumps -> switch (number) {
                case PAGAT -> "Pagat (Trump 1)";
                case ANGEL -> "Angel (Trump 20)";
                case WORLD -> "World (Trump 21)";
                default -> "Trump " + number;
            };
            default -> switch (number) {
                case 1 -> "Ace";
                case KNAVE -> "Knave";
                case CAVALIER -> "Cavalier";
                case QUEEN -> "Queen";
                case KING -> "King";
                default -> String.valueOf(number);
            } + " of " + suit;
        };
    }

    @Override
    public TarotCard copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TarotCard other && suit == other.suit && number == other.number;
    }

    @Override
    public int hashCode() {
        // from the ordinal, as an enum's own hashCode differs between runs
        return Objects.hash(suit.ordinal(), number);
    }

    @Override
    public String toString() {
        return "{" + componentName + "}";
    }
}
