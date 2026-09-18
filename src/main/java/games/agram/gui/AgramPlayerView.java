package games.agram.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.agram.AgramGameState;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static games.agram.gui.AgramGUIManager.*;

/**
 * One player's area: their hand, the number of cards and deals won, and the suits they are known to be void in
 * (public information, from failing to follow suit).
 */
public class AgramPlayerView extends JComponent {

    final int playerId;
    final AgramDeckView handView;
    final int border = 5, borderBottom = 38;   // room for the status line and the titled border below it

    int nCards, dealsWon;
    boolean showDeals;
    String voids = "";

    public AgramPlayerView(Deck<FrenchCard> hand, int playerId) {
        this.playerId = playerId;
        this.handView = new AgramDeckView(playerId, hand, false,
                new Rectangle(border, border, playerAreaWidth, cardHeight));
        this.handView.setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        handView.drawDeck(g2);
        g2.setColor(Color.black);
        String text = nCards + (nCards == 1 ? " card" : " cards")
                + (showDeals ? ",  deals won " + dealsWon : "")
                + (voids.isEmpty() ? "" : ",  void in " + voids);
        g2.drawString(text, border, border + cardHeight + 14);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(playerAreaWidth + border * 2, cardHeight + border + borderBottom);
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

    /** Refresh from the state. {@code showHand} is false when this player's cards are hidden from the viewer. */
    public void update(AgramGameState state, boolean showHand, boolean showDeals) {
        Deck<FrenchCard> hand = state.getPlayerHands().get(playerId);
        handView.updateComponent(hand);
        handView.setFront(showHand);
        nCards = hand.getSize();
        dealsWon = state.getDealsWon(playerId);
        this.showDeals = showDeals;
        StringBuilder sb = new StringBuilder();
        Set<FrenchCard.Suite> known = state.getKnownVoids(playerId);
        for (FrenchCard.Suite suit : FrenchCard.Suite.values())
            if (known.contains(suit))
                sb.append(SUIT_SYMBOLS[suit.ordinal()]);
        voids = sb.toString();
    }

    public AgramDeckView getHandView() {
        return handView;
    }
}
