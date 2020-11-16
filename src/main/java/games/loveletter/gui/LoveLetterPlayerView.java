package games.loveletter.gui;

import core.components.Deck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import javax.swing.*;
import java.awt.*;

import static games.loveletter.gui.LoveLetterGUI.*;


public class LoveLetterPlayerView extends JComponent {

    // ID of player showing
    int playerId;
    // Number of points player has
    int nPoints;

    // Border offsets
    int border = 5;
    int borderBottom = 20;
    int buffer = 10;
    int width, height;

    LoveLetterDeckView handCards;
    LoveLetterDeckView discardCards;

    public LoveLetterPlayerView(Deck<LoveLetterCard> hand, Deck<LoveLetterCard> discard, int playerId, String dataPath) {
        handCards = new LoveLetterDeckView(playerId, hand, false, dataPath,
                new Rectangle(border, border, llCardWidth*2, llCardHeight));
        discardCards = new LoveLetterDeckView(playerId, discard, true, dataPath,
                new Rectangle(border + llCardWidth*2 + buffer, border, playerAreaWidth-llCardWidth*2, llCardHeight));
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
        handCards.drawDeck((Graphics2D) g);
        discardCards.drawDeck((Graphics2D) g);
        // Draw affection tokens
        g.setColor(Color.black);
        g.drawString(nPoints + " affection tokens", border + buffer, border+llCardHeight + buffer);
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
        handCards.updateComponent(gameState.getPlayerHandCards().get(playerId));
        discardCards.updateComponent(gameState.getPlayerDiscardCards().get(playerId));
        nPoints = gameState.getAffectionTokens()[playerId];
    }
}
