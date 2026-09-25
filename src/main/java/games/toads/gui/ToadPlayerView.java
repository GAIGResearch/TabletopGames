package games.toads.gui;

import games.toads.components.ToadCard;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static games.toads.gui.ToadCardArt.cardHeight;
import static games.toads.gui.ToadCardArt.cardWidth;

/**
 * One player's area: two lines of text, their hand, their deck and their Casualty.
 */
public class ToadPlayerView extends JComponent {

    static final int border = 8, gap = 8, textHeight = 44, maxHandSize = 5;
    static final int width = ToadBattleView.width;
    static final int height = border + textHeight + 14 + cardHeight + border;

    final List<ToadCard> hand = new ArrayList<>();
    final List<Boolean> handFaceUp = new ArrayList<>();
    int deckSize;
    String knownDeckCard;
    ToadCard casualty;
    boolean casualtyFaceUp;
    boolean active;
    String[] lines = new String[0];

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // a dark panel behind the cards, so the text is legible against the table
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
        if (active) {
            g2.setColor(new Color(47, 132, 220));
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 12, 12);
            g2.setStroke(new BasicStroke(1));
        }

        g2.setColor(Color.white);
        for (int i = 0; i < lines.length; i++) {
            g2.setFont(g2.getFont().deriveFont(i == 0 ? Font.BOLD : Font.PLAIN, i == 0 ? 15f : 13f));
            g2.drawString(lines[i], border + 4, border + 16 + i * 20);
        }

        int labelY = border + textHeight + 10;
        int top = border + textHeight + 14;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Hand", border, labelY);
        for (int i = 0; i < maxHandSize; i++) {
            Rectangle r = new Rectangle(border + i * (cardWidth + gap), top, cardWidth, cardHeight);
            if (i < hand.size())
                ToadCardArt.draw(g2, hand.get(i), r, handFaceUp.get(i), false);
            else
                ToadCardArt.drawSlot(g2, r);
        }

        // the deck and the Casualty at the right-hand end
        int casualtyX = width - border - cardWidth;
        int deckX = casualtyX - 3 * gap - cardWidth;
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Deck (" + deckSize + ")", deckX, labelY);
        g2.drawString("Casualty", casualtyX, labelY);
        Rectangle deckRect = new Rectangle(deckX, top, cardWidth, cardHeight);
        if (deckSize > 0) {
            ToadCardArt.draw(g2, null, deckRect, false, false);
            if (knownDeckCard != null) {
                g2.setColor(Color.white);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
                g2.drawString("Bottom:", deckX + 10, top + cardHeight / 2 - 4);
                g2.drawString(knownDeckCard, deckX + 10, top + cardHeight / 2 + 10);
            }
        } else {
            ToadCardArt.drawSlot(g2, deckRect);
        }
        Rectangle casualtyRect = new Rectangle(casualtyX, top, cardWidth, cardHeight);
        if (casualty != null)
            ToadCardArt.draw(g2, casualty, casualtyRect, casualtyFaceUp, false);
        else
            ToadCardArt.drawSlot(g2, casualtyRect);
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
     * @param hand           the cards in hand, not including a hidden card already played
     * @param handFaceUp     for each card in hand, whether the viewer may see it
     * @param deckSize       the number of cards in the deck
     * @param knownDeckCard  the name of the bottom card of the deck if the viewer knows it, otherwise null
     * @param casualty       the player's Casualty, or null in War 1
     * @param casualtyFaceUp whether the viewer may see the Casualty
     * @param active         true for the player to act
     * @param lines          the text above the cards; the first is the player's name
     */
    public void update(List<ToadCard> hand, List<Boolean> handFaceUp, int deckSize, String knownDeckCard,
                       ToadCard casualty, boolean casualtyFaceUp, boolean active, String... lines) {
        this.hand.clear();
        this.hand.addAll(hand);
        this.handFaceUp.clear();
        this.handFaceUp.addAll(handFaceUp);
        this.deckSize = deckSize;
        this.knownDeckCard = knownDeckCard;
        this.casualty = casualty;
        this.casualtyFaceUp = casualtyFaceUp;
        this.active = active;
        this.lines = lines;
    }
}
