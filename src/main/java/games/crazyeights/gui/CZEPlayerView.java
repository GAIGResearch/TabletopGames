package games.crazyeights.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.crazyeights.CZEGameState;

import javax.swing.*;
import java.awt.*;

import static games.crazyeights.gui.CZEGUIManager.*;

/**
 * One player's area: their hand, plus the two numbers that matter in Crazy Eights - how many cards they hold
 * (how close they are to going out) and the penalty those cards would score.
 */
public class CZEPlayerView extends JComponent {

    final int playerId;
    final CZEDeckView handView;
    final int border = 5, borderBottom = 38;   // room for the card count AND the titled border below it

    int nCards, penalty;
    boolean penaltyVisible;

    public CZEPlayerView(Deck<FrenchCard> hand, int playerId, String dataPath) {
        this.playerId = playerId;
        this.handView = new CZEDeckView(playerId, hand, false, dataPath,
                new Rectangle(border, border, playerAreaWidth, cardHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        handView.drawDeck((Graphics2D) g);
        g.setColor(Color.black);
        // the penalty is hidden information unless the hand itself is shown
        String text = nCards + (nCards == 1 ? " card" : " cards") + (penaltyVisible ? ",  penalty " + penalty : "");
        g.drawString(text, border, border + cardHeight + 14);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(playerAreaWidth + border * 2, cardHeight + border + borderBottom);
    }

    // the East and West areas are laid out by GridBagLayout, which falls back to the minimum size when the
    // area is shorter than the preferred height - without these the view collapses to 1x1 and vanishes
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** Refresh from the state. {@code showHand} is false when this player's cards are hidden from the viewer. */
    public void update(CZEGameState state, boolean showHand) {
        Deck<FrenchCard> hand = state.getPlayerHands().get(playerId);
        handView.updateComponent(hand);
        handView.setFront(showHand);
        nCards = hand.getSize();
        penalty = state.handPenalty(playerId);
        penaltyVisible = showHand;
    }

    public CZEDeckView getHandView() {
        return handView;
    }
}
