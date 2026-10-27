package games.cribbage.gui;

import core.components.FrenchCard;
import games.cribbage.CribbageGameState;

import javax.swing.*;
import java.awt.*;

import static games.cribbage.gui.CribbageGUIManager.*;

/**
 * One player's area: their hand on the left, the cards they have played this round (face-up) on the right, and a
 * status line with their score and whether they are the dealer.
 */
public class CribbagePlayerView extends JComponent {

    static final int handWidth = cardWidth * 4;
    static final int playedWidth = cardWidth * 3;
    static final int gap = 30;

    final int playerId;
    final CribbageDeckView handView;
    final CribbageDeckView playedView;
    final int border = 5, borderBottom = 38;   // room for the status line and the titled border below it

    int nCards, score;
    boolean dealer;

    public CribbagePlayerView(CribbageGameState state, int playerId, int humanId) {
        this.playerId = playerId;
        handView = new CribbageDeckView(humanId, state.getPlayerHand(playerId), false,
                new Rectangle(border, border + labelHeight, handWidth, cardHeight));
        handView.setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
        playedView = new CribbageDeckView(humanId, state.getPlayedCards(playerId), true,
                new Rectangle(border + handWidth + gap, border + labelHeight, playedWidth, cardHeight));
        playedView.setBottomCardFirst(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.white);
        g2.drawString("Hand", border, border + labelHeight - 4);
        g2.drawString("Played", border + handWidth + gap, border + labelHeight - 4);
        handView.drawDeck(g2);
        playedView.drawDeck(g2);
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        String text = "Score " + score + ",  " + nCards + (nCards == 1 ? " card" : " cards") + (dealer ? ",  dealer (owns the crib)" : "");
        g2.drawString(text, border, border + labelHeight + cardHeight + 16);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(handWidth + gap + playedWidth + border * 2, labelHeight + cardHeight + border + borderBottom);
    }

    // GridBagLayout falls back to the minimum size when an area is too short, which would collapse the view
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** Refresh from the state. {@code showHand} is false when this player's hand is hidden from the viewer. */
    public void update(CribbageGameState state, boolean showHand) {
        handView.updateComponent(state.getPlayerHand(playerId));
        handView.setFront(showHand);
        playedView.updateComponent(state.getPlayedCards(playerId));
        nCards = state.getPlayerHand(playerId).getSize();
        score = state.getScore(playerId);
        // the round counter has moved past the last round once the game is over
        dealer = state.isNotTerminal() && state.getDealer() == playerId;
    }
}
