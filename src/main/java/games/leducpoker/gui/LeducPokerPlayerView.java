package games.leducpoker.gui;

import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;

/**
 * One player's area: their private card (face down unless the viewer may see it) and lines of text about their
 * chips.
 */
public class LeducPokerPlayerView extends JComponent {

    static final int border = 8, gap = 16;
    // as wide as the table view, so the three areas line up
    static final int width = LeducPokerTableView.width;
    static final int height = border + CardArt.cardHeight + border;

    FrenchCard card;
    boolean faceUp;
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
        }

        Rectangle rect = new Rectangle(border, border, CardArt.cardWidth, CardArt.cardHeight);
        if (card == null) {
            g2.setColor(new Color(255, 255, 255, 90));
            g2.drawRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, 8, 8);
        } else if (faceUp) {
            CardArt.drawCardAt(g2, card, rect);
        } else {
            g2.drawImage(CardArt.backOfCard(), rect.x, rect.y, rect.width, rect.height, null);
        }

        int textX = border + CardArt.cardWidth + gap;
        g2.setColor(Color.white);
        for (int i = 0; i < lines.length; i++) {
            g2.setFont(g2.getFont().deriveFont(i == 0 ? Font.BOLD : Font.PLAIN, i == 0 ? 15f : 13f));
            g2.drawString(lines[i], textX, border + 20 + i * 22);
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
     * @param card   the player's private card, or null if they hold none
     * @param faceUp false when the card is hidden from the viewer
     * @param active true for the player to act
     * @param lines  the text beside the card; the first is the player's name
     */
    public void update(FrenchCard card, boolean faceUp, boolean active, String... lines) {
        this.card = card;
        this.faceUp = faceUp;
        this.active = active;
        this.lines = lines;
    }
}
