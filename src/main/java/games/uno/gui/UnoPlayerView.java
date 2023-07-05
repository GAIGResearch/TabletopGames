package games.uno.gui;

import core.components.Deck;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static games.uno.gui.UnoGUIManager.*;

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
        this.width = playerAreaWidth + border*2;
        this.height = playerAreaHeight + border + borderBottom;
        this.playerId = playerId;
        this.playerHandView = new UnoDeckView(playerId, d, true, dataPath, new Rectangle(border, border, playerAreaWidth, unoCardHeight));
    }

    /**
     * Draws the player's hand and their number of points.
     * @param g - Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g) {
        playerHandView.drawDeck((Graphics2D) g);
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
        playerHandView.updateComponent(gameState.getPlayerDecks().get(playerId));
        nPoints = gameState.getPlayerScore()[playerId];
    }
}
