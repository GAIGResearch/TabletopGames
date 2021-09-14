package games.uno.gui;

import core.components.Deck;
import games.uno.cards.UnoCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.uno.gui.UnoGUIManager.*;

public class UnoDeckView extends DeckView<UnoCard> {

    String dataPath;
    Image backOfCard;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     */
    public UnoDeckView(int human, Deck<UnoCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(human, d, visible, unoCardWidth, unoCardHeight, rect);
        this.dataPath = dataPath;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    /**
     * Draws the specified component at the specified place
     *
     * @param g         Graphics object
     * @param rect      Where the item is to be drawn
     * @param card The item itself
     * @param front     true if the item is visible (e.g. the card details); false if only the card-back
     */
    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, UnoCard card, boolean front) {
        Image cardFace = getCardImage(card);
        CardView.drawCard(g, rect, card, cardFace, backOfCard, front);
    }

    /**
     * Retrieves the image for a card from the asset folder.
     * @param card - card to retrieve image for
     * @return - image for card.
     */
    private Image getCardImage(UnoCard card) {
        Image img = null;
        String colorName = card.color.substring(0, 1).toUpperCase() + card.color.substring(1).toLowerCase();
        switch(card.type) {
            case Number:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + card.number + ".png");
                break;
            case Skip:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + "Skip.png");
                break;
            case Reverse:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + "Reverse.png");
                break;
            case Draw:
                img = ImageIO.GetInstance().getImage(dataPath + colorName + "Draw" + card.drawN + ".png");
                break;
            case Wild:
                if (card.drawN > 0) {
                    img = ImageIO.GetInstance().getImage(dataPath + "WildDraw" + card.drawN + ".png");
                } else {
                    img = ImageIO.GetInstance().getImage(dataPath + "Wild.png");
                }
                break;
        }
        return img;
    }
}
