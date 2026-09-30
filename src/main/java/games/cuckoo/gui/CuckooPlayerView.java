package games.cuckoo.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;
import games.tricktaking.gui.FrenchCardDeckView;

import javax.swing.*;
import java.awt.*;

/**
 * One player's seat: their card, and a status line saying whether they are the dealer, and their lives or the round
 * they went out in.
 */
public class CuckooPlayerView extends JComponent {

    static final int areaWidth = 170;
    final int border = 5, borderBottom = 38;   // room for the status line and the titled border below it

    final FrenchCardDeckView cardView;
    String status = "";

    public CuckooPlayerView(Deck<FrenchCard> card, int playerId) {
        // the single card is drawn in the middle of the area
        int cardX = (areaWidth - CardArt.cardWidth) / 2 + border;
        cardView = new FrenchCardDeckView(playerId, card, false,
                new Rectangle(cardX, border, CardArt.cardWidth, CardArt.cardHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // a dark panel behind the card, so the status line is legible against the table
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - borderBottom + 22, 12, 12);
        cardView.drawDeck(g2);
        g2.setColor(Color.white);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(status, (getWidth() - metrics.stringWidth(status)) / 2, border + CardArt.cardHeight + 16);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(areaWidth + border * 2, CardArt.cardHeight + border + borderBottom);
    }

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
     * @param showCard false when this player's card is hidden from the viewer
     * @param status   the status line under the card
     */
    public void update(Deck<FrenchCard> card, boolean showCard, String status) {
        cardView.updateComponent(card);
        cardView.setFront(showCard);
        this.status = status;
    }
}
