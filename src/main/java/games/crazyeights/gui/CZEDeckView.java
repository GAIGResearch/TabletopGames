package games.crazyeights.gui;

import core.components.Deck;
import core.components.FrenchCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

/**
 * Draws a Deck of FrenchCards using the images in data/FrenchCards. Used for every deck in the game -
 * a player's hand, the face-down stock and the face-up discard pile - with {@code front} deciding whether
 * card faces or card backs are shown.
 */
public class CZEDeckView extends DeckView<FrenchCard> {

    final String dataPath;
    final Image backOfCard;

    public CZEDeckView(int humanPlayer, Deck<FrenchCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(humanPlayer, d, visible, CZEGUIManager.cardWidth, CZEGUIManager.cardHeight, rect);
        this.dataPath = dataPath;
        this.backOfCard = ImageIO.GetInstance().getImage(dataPath + "gray_back.png");
    }

    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, FrenchCard card, boolean front) {
        CardView.drawCard(g, rect, card, getCardImage(card), backOfCard, front);
    }

    /**
     * Image file names are &lt;number&gt;&lt;suit&gt;.png for spot cards and &lt;type&gt;&lt;suit&gt;.png for the
     * others (Ace, Jack, Queen, King) - so an Ace is "AceHearts.png", not "14Hearts.png".
     */
    private Image getCardImage(FrenchCard card) {
        if (card == null) return null;
        String name = card.type == FrenchCard.FrenchCardType.Number
                ? card.number + card.suite.name()
                : card.type.name() + card.suite.name();
        return ImageIO.GetInstance().getImage(dataPath + name + ".png");
    }
}
