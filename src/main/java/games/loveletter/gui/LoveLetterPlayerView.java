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
    int buffer = 10;

    Deck<LoveLetterCard> handCards;
    Deck<LoveLetterCard> discardCards;

    public LoveLetterPlayerView(Deck<LoveLetterCard> hand, Deck<LoveLetterCard> discard, int playerId, String dataPath) {
        super(hand, false, dataPath);
        this.handCards = hand;
        this.discardCards = discard;
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
        // Draw hand
        this.component = handCards;
        drawDeck((Graphics2D) g, new Rectangle(border, border, llCardWidth*2, llCardHeight));
        // Draw discard
        this.component = discardCards;
        this.front = true;
        drawDeck((Graphics2D) g, new Rectangle(border + llCardWidth*2 + buffer, border, playerAreaWidth-llCardWidth*2, llCardHeight));
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
        handCards = gameState.getPlayerHandCards().get(playerId);
        discardCards = gameState.getPlayerDiscardCards().get(playerId);
        this.component = handCards;
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
