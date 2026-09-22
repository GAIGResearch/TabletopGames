package games.euchre.gui;

import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;

/**
 * The up-card, with a caption saying what has become of it: face-up while players decide on its suit, then either
 * face-up and taken by the dealer, or face-down once turned down.
 */
public class UpCardView extends JComponent {

    static final int width = CardArt.cardWidth + 60, height = CardArt.cardHeight + 60;

    FrenchCard card;
    boolean faceUp = true;
    String caption = "";

    @Override
    protected void paintComponent(Graphics g) {
        if (card == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(0, 0, width - 1, height - 1, 14, 14);
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
        drawCentred(g2, "Up-card", 16);
        Rectangle rect = new Rectangle((width - CardArt.cardWidth) / 2, 24, CardArt.cardWidth, CardArt.cardHeight);
        if (faceUp)
            CardArt.drawCardAt(g2, card, rect);
        else
            g2.drawImage(CardArt.backOfCard(), rect.x, rect.y, rect.width, rect.height, null);
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        drawCentred(g2, caption, height - 12);
    }

    private void drawCentred(Graphics2D g2, String text, int y) {
        g2.drawString(text, (width - g2.getFontMetrics().stringWidth(text)) / 2, y);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public void update(FrenchCard card, boolean faceUp, String caption) {
        this.card = card;
        this.faceUp = faceUp;
        this.caption = caption;
    }
}
