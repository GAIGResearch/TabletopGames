package games.goofspiel.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * The centre of the table: the face-down prize deck, the prizes on offer (the one turned up this round, plus any
 * carried over from tied rounds), and a few lines of text about the round.
 */
public class GoofspielTableView extends JComponent {

    static final int border = 10, gap = 20;
    static final int offerWidth = 420;
    static final int textWidth = 360;
    static final int width = border + CardArt.cardWidth + gap + offerWidth + gap + textWidth + border;
    static final int height = border + 16 + CardArt.cardHeight + border;

    int prizeDeckSize;
    List<FrenchCard> prizesOnOffer = List.of();
    String[] lines = new String[0];

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

        int top = border + 16;
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString("Prize deck (" + prizeDeckSize + ")", border, border + 10);
        Rectangle deckRect = new Rectangle(border, top, CardArt.cardWidth, CardArt.cardHeight);
        if (prizeDeckSize > 0)
            g2.drawImage(CardArt.backOfCard(), deckRect.x, deckRect.y, deckRect.width, deckRect.height, null);
        else {
            g2.setColor(new Color(255, 255, 255, 90));
            g2.drawRoundRect(deckRect.x, deckRect.y, deckRect.width - 1, deckRect.height - 1, 8, 8);
        }

        int offerX = border + CardArt.cardWidth + gap;
        g2.setColor(Color.white);
        g2.drawString("Prizes on offer", offerX, border + 10);
        // overlap the cards when there are too many to fit side by side; the newest prize is on top (index 0)
        int n = prizesOnOffer.size();
        int step = n <= 1 ? 0 : Math.min(CardArt.cardWidth + 6, (offerWidth - CardArt.cardWidth) / (n - 1));
        for (int i = n - 1; i >= 0; i--) {
            Rectangle r = new Rectangle(offerX + (n - 1 - i) * step, top, CardArt.cardWidth, CardArt.cardHeight);
            CardArt.drawCardAt(g2, prizesOnOffer.get(i), r);
        }

        int textX = offerX + offerWidth + gap;
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
        for (int i = 0; i < lines.length; i++)
            g2.drawString(lines[i], textX, top + 16 + i * 20);
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

    public void update(Deck<FrenchCard> prizeDeck, Deck<FrenchCard> prizesOnOffer, String... lines) {
        this.prizeDeckSize = prizeDeck.getSize();
        this.prizesOnOffer = List.copyOf(prizesOnOffer.getComponents());
        this.lines = lines;
    }
}
