package games.tricktaking.gui;

import core.components.FrenchCard;
import gui.views.CardView;
import utilities.ImageIO;

import java.awt.*;

/**
 * The card images in data/FrenchCards, and the sizes the trick-taking GUIs draw them at.
 */
public class CardArt {

    public static final String dataPath = "data/FrenchCards/";
    // in the order of FrenchCard.Suite: diamond, heart, club, spade
    public static final String[] SUIT_SYMBOLS = {"♦", "♥", "♣", "♠"};

    public static final int cardWidth = 80;
    public static final int cardHeight = 105;

    private CardArt() {
    }

    /**
     * Image file names are &lt;number&gt;&lt;suit&gt;.png for spot cards and &lt;type&gt;&lt;suit&gt;.png for the
     * others - so an Ace is "AceHearts.png", not "14Hearts.png".
     */
    public static Image cardImage(FrenchCard card) {
        if (card == null) return null;
        String name = card.type == FrenchCard.FrenchCardType.Number
                ? card.number + card.suite.name()
                : card.type.name() + card.suite.name();
        return ImageIO.GetInstance().getImage(dataPath + name + ".png");
    }

    public static Image backOfCard() {
        return ImageIO.GetInstance().getImage(dataPath + "gray_back.png");
    }

    /**
     * Draws one card face-up in the given rectangle.
     */
    public static void drawCardAt(Graphics2D g, FrenchCard card, Rectangle rect) {
        CardView.drawCard(g, rect, card, cardImage(card), null, true);
    }

    /**
     * A card in as few characters as possible, e.g. "7♠" or "Q♥".
     */
    public static String shortName(FrenchCard card) {
        String rank = card.type == FrenchCard.FrenchCardType.Number
                ? String.valueOf(card.number)
                : card.type.name().substring(0, 1);
        return rank + SUIT_SYMBOLS[card.suite.ordinal()];
    }

    /**
     * The suit as a symbol and a name, e.g. "♠ Spades".
     */
    public static String suitText(FrenchCard.Suite suit) {
        return SUIT_SYMBOLS[suit.ordinal()] + " " + suit.name();
    }
}
