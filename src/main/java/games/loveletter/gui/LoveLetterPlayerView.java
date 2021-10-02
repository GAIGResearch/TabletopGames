package games.loveletter.gui;

import core.components.Deck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import javax.swing.*;
import java.awt.*;

import static games.loveletter.gui.LoveLetterGUI.*;


public class LoveLetterPlayerView extends JPanel {

    // ID of player
    int playerId;
    // ID of human looking
    int humanId;
    // Number of points player has
    int nPoints;

    // Border offsets
    int border = 5;
    int borderBottom = 25;
    int buffer = 10;
    int width, height;

    LoveLetterDeckView handCards;
    LoveLetterDeckView discardCards;

    public LoveLetterPlayerView(Deck<LoveLetterCard> hand, Deck<LoveLetterCard> discard, int playerId, int humanId, String dataPath) {
        handCards = new LoveLetterDeckView(humanId, hand, false, dataPath,
                new Rectangle(0, 0, playerAreaWidth / 2 - border * 2, llCardHeight));
        discardCards = new LoveLetterDeckView(humanId, discard, true, dataPath,
                new Rectangle(0, 0, playerAreaWidth / 2 - border * 2, llCardHeight));
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.add(handCards);
        this.add(discardCards);
        this.width = playerAreaWidth + border * 2;
        this.height = playerAreaHeight + border + borderBottom;
        this.humanId = humanId;
        this.playerId = playerId;
    }

    /**
     * Draws the player's hand and their number of points.
     *
     * @param g - Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Draw affection tokens
        g.setColor(Color.black);
        g.drawString(nPoints + " affection tokens", border + buffer, llCardHeight + borderBottom);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Updates information
     *
     * @param gameState - current game state
     */
    public void update(LoveLetterGameState gameState) {
        handCards.updateComponent(gameState.getPlayerHandCards().get(playerId));
        discardCards.updateComponent(gameState.getPlayerDiscardCards().get(playerId));
        nPoints = gameState.getAffectionTokens()[playerId];
    }
}
