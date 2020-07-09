package games.loveletter.gui;

import core.components.Deck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.awt.*;

import static games.loveletter.gui.LoveLetterGUI.*;


public class LoveLetterPlayerView extends LoveLetterDeckView {

    // ID of player showing
    int playerId;
    // Number of points player has
    int nPoints;

    // Border offsets
    int border = 5;
    int borderBottom = 20;

    public LoveLetterPlayerView(Deck<LoveLetterCard> d, int playerId, String dataPath) {
        super(d, false, dataPath);
        this.width = playerAreaWidth + border*2;
        this.height = playerAreaHeight + border + borderBottom;
        this.playerId = playerId;
    }

    /**
     * Draws the player's hand and their number of points.
     * @param g - Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D) g, new Rectangle(border, border, playerAreaWidth, llCardHeight));
        g.setColor(Color.black);
        g.drawString(nPoints + " points", border+playerAreaWidth/2 - 20, border+llCardHeight + 10);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Updates information
     * @param gameState - current game state
     */
    public void update(LoveLetterGameState gameState) {
        this.component = gameState.getPlayerHandCards().get(playerId);
        nPoints = gameState.getAffectionTokens()[playerId];
    }

    // Getters, setters
    public void setFront(boolean visible) {
        this.front = visible;
    }
    public void flip() {
        front = !front;
    }
}
