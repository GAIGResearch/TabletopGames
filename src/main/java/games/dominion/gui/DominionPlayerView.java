package games.dominion.gui;

import games.dominion.*;

import javax.swing.*;
import java.awt.*;

import static games.dominion.DominionConstants.*;
import static games.dominion.gui.DominionGUI.*;

public class DominionPlayerView extends JComponent {

    DominionDeckView playerHand;
    DominionDeckView playerDiscard;
    DominionDeckView playerDraw;
    DominionDeckView playerTableau;

    // Width and height of display area
    int width, height;
    // ID of player this is showing
    int playerId;

    int actions;
    int buys;
    int spendAvailable;

    // Border offsets
    int border = 5;
    int textHeight = 20;
    int handWidth = playerAreaWidth - cardWidth - border * 2;

    public DominionPlayerView(int playerId, String dataPath, DominionGameState state) {
        this.playerId = playerId;
        this.width = playerAreaWidth + border * 2;
        this.height = playerAreaHeight + textHeight + border;
        playerHand = new DominionDeckView(playerId, state.getDeck(DeckType.HAND, playerId), false, dataPath,
                new Rectangle(border, border * 2 + cardHeight + textHeight * 2, handWidth, cardHeight));
        playerDiscard = new DominionDeckView(playerId, state.getDeck(DeckType.DISCARD, playerId), true, dataPath,
                new Rectangle(border * 2 + handWidth, border + textHeight, cardWidth, cardHeight));
        playerDiscard.minCardOffset = 0;
        playerDraw = new DominionDeckView(playerId, state.getDeck(DeckType.DRAW, playerId), false, dataPath,
                new Rectangle(border * 2 + handWidth, border * 2 + cardHeight + textHeight * 2, cardWidth, cardHeight));
        playerDraw.minCardOffset = 0;
        playerTableau = new DominionDeckView(playerId, state.getDeck(DeckType.TABLE, playerId), true, dataPath,
                new Rectangle(border, border + textHeight, handWidth, cardHeight));
    }

    /**
     * Draws the player's hand and their number of points.
     *
     * @param g - Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g) {
        playerHand.drawDeck((Graphics2D) g);
        playerDiscard.drawDeck((Graphics2D) g);
        playerDraw.drawDeck((Graphics2D) g);
        playerTableau.drawDeck((Graphics2D) g);

        g.setFont(Font.getFont(Font.SANS_SERIF));
        g.setColor(Color.gray);

        g.drawString("Hand", border, cardHeight + textHeight * 2 + border);
        g.drawString("Discard", border * 2 + handWidth, textHeight);
        g.drawString("Drawpile", border * 2 + handWidth, cardHeight + textHeight * 2 + border);
        g.drawString("Played Cards", border, textHeight);

        g.setColor(Color.black);
        String infoString = String.format("Actions: %d, Buys: %d, AvailableSpend: %d", actions, buys, spendAvailable);
        g.drawString(infoString, border, playerAreaHeight);
    }

    public void update(DominionGameState state) {
        // There is no need to updateComponent on the DeckViews, as once initialised, the same Deck is retained throughout a Game
        // (if we ignore the copies of GameState used in MCTS etc.)
        if (state.getCurrentPlayer() == playerId) {
            actions = state.actionsLeft();
            spendAvailable = state.availableSpend(playerId);
            buys = state.buysLeft();
        } else {
            actions = 0;
            spendAvailable = 0;
            buys = 0;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

}
