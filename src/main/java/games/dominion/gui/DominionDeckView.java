package games.dominion.gui;

import core.components.Deck;
import games.dominion.cards.DominionCard;
import gui.views.*;
import utilities.ImageIO;

import java.awt.*;

import static games.dominion.gui.DominionGUIManager.*;

public class DominionDeckView extends DeckView<DominionCard> {
    // Back of card image
    Image backOfCard;
    // Path to assets
    String dataPath;

    /**
     * Constructor initialising information and adding key/mouse listener for card highlight (left click or ALT + hover
     * allows showing the highlighted card on top of all others).
     *
     * @param d        - deck to draw
     * @param visible  - true if whole deck visible
     * @param dataPath - path to assets
     */
    public DominionDeckView(int player, Deck<DominionCard> d, boolean visible, String dataPath, Rectangle rect) {
        super(player, d, visible, cardWidth, cardHeight, rect);
        this.front = visible;
        backOfCard = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
        this.dataPath = dataPath;
    }

    /**
     * Draws the specified component at the specified place
     *
     * @param g     Graphics object
     * @param rect  Where the item is to be drawn
     * @param card  The item itself
     * @param front true if the item is visible (e.g. the card details); false if only the card-back
     */
    @Override
    public void drawComponent(Graphics2D g, Rectangle rect, DominionCard card, boolean front) {
        // This will draw a blank rectangle for the front of the card
        CardView.drawCard(g, rect, card, null, backOfCard, front);
    }
}
