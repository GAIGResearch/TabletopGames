package games.tricktaking.gui;

import core.components.Deck;
import core.components.FrenchCard;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * One player's area: their hand, and a status line with the number of cards, whatever the game adds (tricks won,
 * deals won) and the suits they are known to be void in.
 */
public class PlayerHandView extends JComponent {

    final int playerAreaWidth;
    final FrenchCardDeckView handView;
    final int border = 5, borderBottom = 38;   // room for the status line and the titled border below it

    int nCards;
    String status = "";

    public PlayerHandView(Deck<FrenchCard> hand, int playerId, int playerAreaWidth) {
        this.playerAreaWidth = playerAreaWidth;
        this.handView = new FrenchCardDeckView(playerId, hand, false,
                new Rectangle(border, border, playerAreaWidth, CardArt.cardHeight));
        this.handView.setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // a dark panel behind the cards, so the status line is legible against the table
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - borderBottom + 22, 12, 12);
        handView.drawDeck(g2);
        g2.setColor(Color.white);
        g2.drawString(status, border, border + CardArt.cardHeight + 16);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(playerAreaWidth + border * 2, CardArt.cardHeight + border + borderBottom);
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

    /**
     * Refresh from the state.
     *
     * @param showHand false when this player's cards are hidden from the viewer
     * @param voids    the suits the player is known to be void in
     * @param extra    what the game adds to the status line (empty for nothing)
     */
    public void update(Deck<FrenchCard> hand, boolean showHand, Set<FrenchCard.Suite> voids, String extra) {
        handView.updateComponent(hand);
        handView.setFront(showHand);
        nCards = hand.getSize();
        StringBuilder voidText = new StringBuilder();
        for (FrenchCard.Suite suit : FrenchCard.Suite.values())
            if (voids.contains(suit))
                voidText.append(CardArt.SUIT_SYMBOLS[suit.ordinal()]);
        status = nCards + (nCards == 1 ? " card" : " cards")
                + (extra.isEmpty() ? "" : ",  " + extra)
                + (voidText.isEmpty() ? "" : ",  void in " + voidText);
    }

    public FrenchCardDeckView getHandView() {
        return handView;
    }
}
