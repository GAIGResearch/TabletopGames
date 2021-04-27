package games.sushigo.gui;

import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import javax.swing.*;

import java.awt.*;

import static games.sushigo.gui.SGGUI.*;


public class SGPlayerView extends JComponent {

    // ID of player showing
    int playerId;
    // Number of points player has
    int nPoints;
    SGDeckView playerHandView;
    // Border offsets
    int border = 5;
    int borderBottom = 20;
    int width, height;

    public SGPlayerView(Deck<SGCard> deck, int playerId, int humanId, String dataPath)
    {
        this.width = playerAreaWidth + border*2;
        this.height = playerAreaHeight + border + borderBottom;
        this.playerId = playerId;
        this.playerHandView = new SGDeckView(humanId, deck, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        playerHandView.drawDeck((Graphics2D) g);
        g.setColor(Color.black);
        g.drawString(nPoints + " points", border+playerAreaWidth/2 - 20, border + SGCardHeight+10);
    }

    public Dimension getPrefferedSize()
    {
        return new Dimension(width, height);
    }

    public void update(SGGameState gameState)
    {
        playerHandView.updateComponent(gameState.getPlayerDecks().get(playerId));
        nPoints = gameState.getPlayerScore()[playerId];
    }
}
