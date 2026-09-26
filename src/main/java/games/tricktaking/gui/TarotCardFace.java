package games.tricktaking.gui;

import core.components.TarotCard;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

/**
 * Draws tarot cards, which have no images, coloured by suit.
 */
public class TarotCardFace implements CardFace<TarotCard, TarotCard.Suit> {

    public static final int cardWidth = 58, cardHeight = 88;

    static final Color[] SUIT_COLOURS = {
            new Color(40, 70, 150),    // Swords
            new Color(30, 110, 40),    // Batons
            new Color(180, 30, 30),    // Cups
            new Color(170, 120, 0),    // Coins
            new Color(110, 40, 140),   // Trumps
            Color.black                // the Fool
    };
    static final String[] SUIT_SYMBOLS = {"Sw", "Ba", "Cu", "Co", "Tr", ""};

    final Comparator<? super TarotCard> handOrder;

    /**
     * @param handOrder the order to show a face-up hand in
     */
    public TarotCardFace(Comparator<? super TarotCard> handOrder) {
        this.handOrder = handOrder;
    }

    @Override
    public int cardWidth() {
        return cardWidth;
    }

    @Override
    public int cardHeight() {
        return cardHeight;
    }

    @Override
    public void drawFront(Graphics2D g, TarotCard card, Rectangle r) {
        Color colour = SUIT_COLOURS[card.suit.ordinal()];
        g.setColor(Color.white);
        g.fillRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
        g.setColor(colour);
        g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
        g.drawRoundRect(r.x + 3, r.y + 3, r.width - 7, r.height - 7, 6, 6);

        // the corners: rank over suit. A hand overlaps each card with the one to its left, leaving only the right
        // edge showing, so the right-hand corner is the one that identifies it there
        Font font = g.getFont();
        String rank = rankText(card), suit = SUIT_SYMBOLS[card.suit.ordinal()];
        g.setFont(font.deriveFont(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(rank, r.x + 6, r.y + 16);
        g.drawString(rank, r.x + r.width - 5 - fm.stringWidth(rank), r.y + 16);
        g.setFont(font.deriveFont(Font.PLAIN, 10f));
        fm = g.getFontMetrics();
        g.drawString(suit, r.x + 6, r.y + 28);
        g.drawString(suit, r.x + r.width - 5 - fm.stringWidth(suit), r.y + 28);

        // the middle: a plain oval in the suit's colour (no text, which would show in pieces where a hand overlaps
        // the card), and the name of a named card at the bottom left, where a hand hides it rather than cutting it
        g.fillOval(r.x + r.width / 2 - 9, r.y + r.height / 2 - 10, 18, 24);
        String name = card.isFool() ? "Fool" : cardName(card);
        if (!name.isEmpty()) {
            g.setFont(font.deriveFont(Font.PLAIN, 10f));
            g.drawString(name, r.x + 6, r.y + r.height - 10);
        }
        g.setFont(font);
    }

    @Override
    public void drawBack(Graphics2D g, Rectangle r) {
        g.setColor(new Color(60, 60, 110));
        g.fillRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
        g.setColor(new Color(200, 190, 140));
        g.drawRoundRect(r.x + 3, r.y + 3, r.width - 7, r.height - 7, 6, 6);
        for (int d = 10; d < r.width + r.height; d += 10)
            g.drawLine(r.x + Math.max(3, d - r.height + 4), r.y + Math.min(r.height - 4, d),
                    r.x + Math.min(r.width - 4, d), r.y + Math.max(3, d - r.width + 4));
        g.setColor(Color.black);
        g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
    }

    /**
     * The rank in as few characters as possible: K, Q, C (Cavalier), J (Knave) or the number; trumps by number.
     */
    public static String rankText(TarotCard card) {
        if (card.isFool())
            return "0";
        if (card.isTrump())
            return String.valueOf(card.number);
        return switch (card.number) {
            case TarotCard.KING -> "K";
            case TarotCard.QUEEN -> "Q";
            case TarotCard.CAVALIER -> "C";
            case TarotCard.KNAVE -> "J";
            default -> String.valueOf(card.number);
        };
    }

    /**
     * A card in as few characters as possible, e.g. "K Cu", "7 Sw", "Tr 20" or "Fool".
     */
    public static String shortName(TarotCard card) {
        if (card.isFool())
            return "Fool";
        if (card.isTrump())
            return "Tr " + card.number;
        return rankText(card) + " " + SUIT_SYMBOLS[card.suit.ordinal()];
    }

    private static String cardName(TarotCard card) {
        if (card.isFool())
            return "";
        // the court cards need no name: their corners say K, Q, C or J
        if (card.isTrump())
            return switch (card.number) {
                case TarotCard.PAGAT -> "Pagat";
                case TarotCard.ANGEL -> "Angel";
                case TarotCard.WORLD -> "World";
                default -> "";
            };
        return "";
    }

    @Override
    public String suitSymbol(TarotCard.Suit suit) {
        return SUIT_SYMBOLS[suit.ordinal()];
    }

    @Override
    public List<TarotCard.Suit> suits() {
        return List.of(TarotCard.Suit.Swords, TarotCard.Suit.Batons, TarotCard.Suit.Cups, TarotCard.Suit.Coins,
                TarotCard.Suit.Trumps);
    }

    @Override
    public Comparator<? super TarotCard> handOrder() {
        return handOrder;
    }
}
