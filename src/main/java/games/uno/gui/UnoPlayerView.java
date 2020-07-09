package games.uno.gui;

import core.components.Deck;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;

import java.awt.*;

import static games.uno.gui.UnoGUI.*;

public class UnoPlayerView extends UnoDeckView {

    // ID of player showing
    int playerId;
    // Number of points player has
    int nPoints;

    // Border offsets
    int border = 5;
    int borderBottom = 20;

    public UnoPlayerView(Deck<UnoCard> d, int playerId, String dataPath) {
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
        drawDeck((Graphics2D) g, new Rectangle(border, border, playerAreaWidth, unoCardHeight));
        g.setColor(Color.black);
        g.drawString(nPoints + " points", border+playerAreaWidth/2 - 20, border+unoCardHeight + 10);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Updates information
     * @param gameState - current game state
     */
    public void update(UnoGameState gameState) {
        this.component = gameState.getPlayerDecks().get(playerId);
        nPoints = gameState.getPlayerScore()[playerId];
    }

    // Getters, setters
    public void setFront(boolean visible) {
        this.front = visible;
    }
    public void flip() {
        front = !front;
    }
}
