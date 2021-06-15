package games.poker.gui;

import core.components.Deck;
import games.poker.PokerGameState;
import core.components.FrenchCard;

import java.awt.*;

import static games.poker.gui.PokerGUI.*;

public class PokerPlayerView extends PokerDeckView {

    // ID of player showing
    int playerId;
    // Number of points player has
    //int nPoints;

    // Border offsets
    int border = 5;
    int borderBottom = 20;

    public PokerPlayerView(Deck<FrenchCard> d, int playerId, String dataPath) {
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
        drawDeck((Graphics2D) g, new Rectangle(border, border, playerAreaWidth, pokerCardHeight));
        g.setColor(Color.black);
        //g.drawString(nPoints + " points", border+playerAreaWidth/2 - 20, border+pokerCardHeight + 10);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Updates information
     * @param gameState - current game state
     */
    public void update(PokerGameState gameState) {
        this.component = gameState.getPlayerDecks().get(playerId);
        //nPoints = gameState.getPlayerScore()[playerId];
    }

    // Getters, setters
    public void setFront(boolean visible) {
        this.front = visible;
    }
    public void flip() {
        front = !front;
    }
}
