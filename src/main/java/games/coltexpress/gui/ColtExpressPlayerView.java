package games.coltexpress.gui;

import core.components.Deck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.ColtExpressCard;

import java.awt.*;
import java.util.HashMap;

import static games.coltexpress.gui.ColtExpressGUI.playerAreaHeight;
import static games.coltexpress.gui.ColtExpressGUI.playerAreaWidth;

public class ColtExpressPlayerView extends ColtExpressDeckView {

    int width;
    int height;
    int playerId;

    public ColtExpressPlayerView(Deck<ColtExpressCard> d, int playerId, String dataPath,
                                 HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        super(d, false, dataPath, characters);
        this.width = playerAreaWidth;
        this.height = playerAreaHeight;
        this.playerId = playerId;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D) g);
        g.setColor(Color.black);
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

    public void update(ColtExpressGameState gameState) {
        this.component = gameState.getPlayerDecks().get(playerId);
    }
}
