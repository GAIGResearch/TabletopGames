package games.poker.gui;

import core.components.Deck;
import games.poker.PokerGameState;
import core.components.FrenchCard;

import java.awt.*;

import static games.poker.gui.PokerGUIManager.*;

public class PokerPlayerView extends PokerDeckView {

    // ID of player showing
    int playerId, bet;

    // Border offsets
    int border = 5;
    int borderBottom = 20;

    boolean firstPlayerOfRound = false;

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
        g.setColor(new Color(87, 37, 25));
        Font f = g.getFont();
        g.setFont(new Font(f.getName(), Font.BOLD, 30));
        if (firstPlayerOfRound) {
            g.drawString("*", border + playerAreaWidth / 2 - 20, border + pokerCardHeight + 25);
        }
        g.drawString("" + bet, playerAreaWidth - 50, playerAreaHeight/2);
        g.setFont(f);
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
        firstPlayerOfRound = gameState.getFirstPlayer() == playerId;
        bet = gameState.getPlayerBet()[playerId].getValue();
    }

    // Getters, setters
    public void setFront(boolean visible) {
        this.front = visible;
    }
    public void flip() {
        front = !front;
    }
}
