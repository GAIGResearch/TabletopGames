package games.agram.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

/**
 * Draws a Deck of FrenchCards using the images in data/FrenchCards, with {@code front} deciding whether card faces
 * or card backs are shown.
 */
public class AgramDeckView extends DeckView<FrenchCard> {

    final Image backOfCard;

    public AgramDeckView(int humanPlayer, Deck<FrenchCard> d, boolean visible, Rectangle rect) {
        super(humanPlayer, d, visible, AgramGUIManager.cardWidth, AgramGUIManager.cardHeight, rect);
        this.backOfCard = ImageIO.GetInstance().getImage(AgramGUIManager.dataPath + "gray_back.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        CardView.drawCard(g, rect, card, cardImage(card), backOfCard, front);
    }

    /** Draw one card face-up in the given rectangle. */
    static void drawCardAt(Graphics2D g, FrenchCard card, Rectangle rect) {
        CardView.drawCard(g, rect, card, cardImage(card), null, true);
    }

    /**
     * Image file names are &lt;number&gt;&lt;suit&gt;.png for spot cards and &lt;type&gt;&lt;suit&gt;.png for the
     * others - so an Ace is "AceHearts.png", not "14Hearts.png".
     */
    static Image cardImage(FrenchCard card) {
        if (card == null) return null;
        String name = card.type == FrenchCard.FrenchCardType.Number
                ? card.number + card.suite.name()
                : card.type.name() + card.suite.name();
        return ImageIO.GetInstance().getImage(AgramGUIManager.dataPath + name + ".png");
    }
}
