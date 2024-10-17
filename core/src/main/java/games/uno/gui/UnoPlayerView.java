package games.uno.gui;

import games.uno.cards.UnoCard;
import core.components.Deck;
import games.uno.UnoGameState;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class UnoPlayerView extends JComponent {

    // ID of player showing
    int playerId;
    // Number of points player has
    int nPoints;
    UnoDeckView playerHandView;
    // Border offsets
    int border = 5;
    int borderBottom = 20;
    int width, height;

    public UnoPlayerView(Deck<UnoCard> d, int playerId, Set<Integer> humanId, String dataPath) {
        this.width = UnoGUIManager.playerAreaWidth + border*2;
        this.height = UnoGUIManager.playerAreaHeight + border + borderBottom;
        this.playerId = playerId;
        this.playerHandView = new UnoDeckView(playerId, d, true, dataPath, new Rectangle(border, border, UnoGUIManager.playerAreaWidth, UnoGUIManager.unoCardHeight));
    }

    /**
     * Draws the player's hand and their number of points.
     * @param g - Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g) {
        playerHandView.drawDeck((Graphics2D) g);
        g.setColor(Color.black);
        g.drawString(nPoints + " points", border+ UnoGUIManager.playerAreaWidth/2 - 20, border+ UnoGUIManager.unoCardHeight + 10);
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
        playerHandView.updateComponent(gameState.getPlayerDecks().get(playerId));
        nPoints = gameState.getPlayerScore()[playerId];
    }
}
