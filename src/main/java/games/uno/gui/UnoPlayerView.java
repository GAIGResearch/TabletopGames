package games.uno.gui;

import core.components.Deck;
import games.uno.UnoGameState;
import games.uno.cards.UnoCard;

import java.awt.*;

import static games.uno.gui.UnoGUI.*;

public class UnoPlayerView extends UnoDeckView {

    int width;
    int height;
    int playerId;
    int nPoints;

    public UnoPlayerView(Deck<UnoCard> d, int playerId, String dataPath) {
        super(d, false, dataPath);
        this.width = playerAreaWidth;
        this.height = playerAreaHeight;
        this.playerId = playerId;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D) g);
        g.setColor(Color.black);
        g.drawString(nPoints + " points", playerAreaWidth/2 - 20, unoCardHeight + 10);
    }

    public void setFront(boolean visible) {
        this.front = visible;
    }

    public void flip() {
        front = !front;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(UnoGameState gameState) {
        this.component = gameState.getPlayerDecks().get(playerId);
        nPoints = gameState.getPlayerScore()[playerId];
    }
}
