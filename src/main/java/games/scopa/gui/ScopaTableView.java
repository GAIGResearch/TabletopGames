package games.scopa.gui;

import core.components.Deck;
import core.components.TarotCard;
import games.tricktaking.gui.TarotCardFace;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * The middle of the table: a header line, the face-up table cards side by side (in two rows when there are many), and
 * a line underneath for the draw deck and the scores.
 */
public class ScopaTableView extends JComponent {

    static final int perRow = 12, gap = 6, border = 8, lineHeight = 20;

    final TarotCardFace face;
    List<TarotCard> cards = List.of();
    String header = "", footer = "";

    public ScopaTableView(TarotCardFace face) {
        this.face = face;
    }

    public void update(Deck<TarotCard> table, String header, String footer) {
        this.cards = List.copyOf(table.getComponents());
        this.header = header;
        this.footer = footer;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
        g.setColor(Color.white);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        g.drawString(header, border, border + 14);
        int top = border + lineHeight + 4;
        if (cards.isEmpty()) {
            g.setFont(g.getFont().deriveFont(Font.ITALIC, 13f));
            g.drawString("The table is empty", border, top + face.cardHeight() / 2);
        }
        for (int i = 0; i < cards.size(); i++) {
            int row = i / perRow, col = i % perRow;
            face.drawFront(g, cards.get(i), new Rectangle(border + col * (face.cardWidth() + gap),
                    top + row * (face.cardHeight() + gap), face.cardWidth(), face.cardHeight()));
        }
        int rows = Math.max(1, (cards.size() + perRow - 1) / perRow);
        g.setColor(Color.white);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 13f));
        g.drawString(footer, border, top + rows * (face.cardHeight() + gap) + 14);
    }

    @Override
    public Dimension getPreferredSize() {
        // room for two rows of cards between the header and the footer
        return new Dimension(border * 2 + perRow * (face.cardWidth() + gap),
                border * 2 + 2 * lineHeight + 8 + 2 * (face.cardHeight() + gap));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
