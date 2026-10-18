package games.leducpoker.gui;

import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;

/**
 * The centre of the table: the face-down draw deck, the board card, and lines of text about the hand and the pot.
 */
public class LeducPokerTableView extends JComponent {

    static final int border = 10, gap = 20, textWidth = 300;
    static final int width = border + CardArt.cardWidth + gap + CardArt.cardWidth + gap + textWidth + border;
    static final int height = border + 16 + CardArt.cardHeight + border;

    int drawDeckSize;
    FrenchCard boardCard;
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
        g2.drawString("Draw deck (" + drawDeckSize + ")", border, border + 10);
        Rectangle deckRect = new Rectangle(border, top, CardArt.cardWidth, CardArt.cardHeight);
        if (drawDeckSize > 0)
            g2.drawImage(CardArt.backOfCard(), deckRect.x, deckRect.y, deckRect.width, deckRect.height, null);
        else
            drawEmptySlot(g2, deckRect);

        int boardX = border + CardArt.cardWidth + gap;
        g2.setColor(Color.white);
        g2.drawString("Board", boardX, border + 10);
        Rectangle boardRect = new Rectangle(boardX, top, CardArt.cardWidth, CardArt.cardHeight);
        if (boardCard != null)
            CardArt.drawCardAt(g2, boardCard, boardRect);
        else
            drawEmptySlot(g2, boardRect);

        int textX = boardX + CardArt.cardWidth + gap;
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
        for (int i = 0; i < lines.length; i++)
            g2.drawString(lines[i], textX, top + 16 + i * 20);
    }

    private static void drawEmptySlot(Graphics2D g, Rectangle rect) {
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(rect.x, rect.y, rect.width - 1, rect.height - 1, 8, 8);
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

    /**
     * @param drawDeckSize the number of cards in the draw deck
     * @param boardCard    the board card, or null before it is dealt
     * @param lines        the text beside the cards
     */
    public void update(int drawDeckSize, FrenchCard boardCard, String... lines) {
        this.drawDeckSize = drawDeckSize;
        this.boardCard = boardCard;
        this.lines = lines;
    }
}
