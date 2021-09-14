package games.loveletter.gui;

import core.components.Deck;
import games.loveletter.cards.LoveLetterCard;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import java.awt.*;

import static games.loveletter.gui.LoveLetterGUIManager.*;

public class LoveLetterDeckView extends DeckView<LoveLetterCard> {

    // Back of card image
    Image backOfCard;
    // Path to assets
    String dataPath;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     * @param d - deck to draw
     * @param visible - true if whole deck visible
     * @param dataPath - path to assets
     * @param rect - the location of the Deck
     */
    public LoveLetterDeckView(int human, Deck<LoveLetterCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(human, d, visible, llCardWidth, llCardHeight, rect);
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;
    }
    public LoveLetterDeckView(int player, Deck<LoveLetterCard> d, boolean visible, String dataPath) {
        this(player, d, visible, dataPath, new Rectangle(0, 0, llCardWidth, llCardHeight));
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
    public void drawComponent(Graphics2D g, Rectangle rect, LoveLetterCard card, boolean front) {
        Image cardFace = ImageIO.GetInstance().getImage(dataPath + card.cardType.name().toLowerCase() + ".png");
        CardView.drawCard(g, rect, card, cardFace, backOfCard, front);
    }

}
