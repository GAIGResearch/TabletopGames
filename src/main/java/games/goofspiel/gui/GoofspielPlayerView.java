package games.goofspiel.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.FrenchCardDeckView;

import javax.swing.*;
import java.awt.*;

/**
 * One player's area: their hand, the card they have bid this round (face down until the bids are revealed), the
 * bid they revealed last round, and a status line with the prizes they have won and their score.
 */
public class GoofspielPlayerView extends JComponent {

    static final int handWidth = 300;
    static final int border = 5, gap = 12, statusHeight = 22;
    static final int width = border + handWidth + gap + CardArt.cardWidth + gap + CardArt.cardWidth + border;
    static final int height = border + 14 + CardArt.cardHeight + statusHeight + border;

    final FrenchCardDeckView handView;
    final Rectangle bidRect, lastBidRect;

    FrenchCard bid, lastBid;
    boolean showBid;
    String status = "";

    public GoofspielPlayerView(Deck<FrenchCard> hand, int playerId) {
        int top = border + 14;
        handView = new FrenchCardDeckView(playerId, hand, false, new Rectangle(border, top, handWidth, CardArt.cardHeight));
        handView.setDisplayOrder(FrenchCard.HAND_DISPLAY_ORDER);
        bidRect = new Rectangle(border + handWidth + gap, top, CardArt.cardWidth, CardArt.cardHeight);
        lastBidRect = new Rectangle(bidRect.x + CardArt.cardWidth + gap, top, CardArt.cardWidth, CardArt.cardHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // a dark panel behind the cards, so the text is legible against the table
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Hand", border + 2, border + 10);
        g2.drawString("Bid", bidRect.x + 2, border + 10);
        g2.drawString("Last bid", lastBidRect.x + 2, border + 10);

        handView.drawDeck(g2);
        drawSlot(g2, bidRect, bid, showBid);
        drawSlot(g2, lastBidRect, lastBid, true);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString(status, border + 2, border + 14 + CardArt.cardHeight + 16);
    }

    /**
     * A card face up or face down, or an empty outline when there is none.
     */
    private static void drawSlot(Graphics2D g, Rectangle rect, FrenchCard card, boolean faceUp) {
        if (card == null) {
            g.setColor(new Color(255, 255, 255, 90));
            g.drawRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, 8, 8);
        } else if (faceUp) {
            CardArt.drawCardAt(g, card, rect);
        } else {
            g.drawImage(CardArt.backOfCard(), rect.x, rect.y, rect.width, rect.height, null);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
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
     * @param showCards false when this player's hand and bid are hidden from the viewer
     * @param bid       the card bid this round, or null if the player has not bid
     * @param lastBid   the bid revealed last round, or null before the first round is resolved
     * @param status    the status line under the cards
     */
    public void update(Deck<FrenchCard> hand, boolean showCards, FrenchCard bid, FrenchCard lastBid, String status) {
        handView.updateComponent(hand);
        handView.setFront(showCards);
        this.bid = bid;
        this.showBid = showCards;
        this.lastBid = lastBid;
        this.status = status;
    }
}
