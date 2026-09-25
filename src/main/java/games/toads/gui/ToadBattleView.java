package games.toads.gui;

import games.toads.components.ToadCard;

import javax.swing.*;
import java.awt.*;

import static games.toads.gui.ToadCardArt.cardHeight;
import static games.toads.gui.ToadCardArt.cardWidth;

/**
 * The centre of the table: the two lanes of the Battle, with each player's card in them, and lines of text about
 * the War. When no card has been played yet, the cards of the last Battle are shown dimmed.
 */
public class ToadBattleView extends JComponent {

    static final int border = 10, gap = 8, laneGap = 28, textWidth = 360;
    static final int width = border + 2 * (2 * cardWidth + gap) + laneGap + 24 + textWidth + border;
    static final int height = border + 34 + cardHeight + border;

    // cards[player][lane], lane 0 the face-up lane and lane 1 the hidden lane
    final ToadCard[][] cards = new ToadCard[2][2];
    final boolean[][] faceUp = new boolean[2][2];
    boolean lastBattle;
    String[] lines = new String[0];

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

        int top = border + 34;
        for (int lane = 0; lane < 2; lane++) {
            int laneX = border + lane * (2 * cardWidth + gap + laneGap);
            g2.setColor(Color.white);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
            g2.drawString((lane == 0 ? "Face-up lane" : "Hidden lane") + (lastBattle ? " (last Battle)" : ""),
                    laneX, border + 12);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
            // player 1 on the left, as their area is at the top, then player 0
            for (int i = 0; i < 2; i++) {
                int player = 1 - i;
                int x = laneX + i * (cardWidth + gap);
                g2.setColor(Color.white);
                g2.drawString("Player " + player, x, border + 28);
                Rectangle r = new Rectangle(x, top, cardWidth, cardHeight);
                if (cards[player][lane] != null)
                    ToadCardArt.draw(g2, cards[player][lane], r, faceUp[player][lane], lastBattle);
                else
                    ToadCardArt.drawSlot(g2, r);
            }
        }

        int textX = width - border - textWidth;
        g2.setColor(Color.white);
        for (int i = 0; i < lines.length; i++) {
            g2.setFont(g2.getFont().deriveFont(i == 0 ? Font.BOLD : Font.PLAIN, i == 0 ? 14f : 13f));
            g2.drawString(lines[i], textX, border + 16 + i * 20);
        }
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
     * @param cards      cards[player][lane], lane 0 the face-up lane and lane 1 the hidden lane; null where none
     * @param faceUp     whether the viewer may see each card
     * @param lastBattle true when the cards are those of the last Battle rather than the current one
     * @param lines      the text beside the lanes
     */
    public void update(ToadCard[][] cards, boolean[][] faceUp, boolean lastBattle, String... lines) {
        for (int p = 0; p < 2; p++) {
            System.arraycopy(cards[p], 0, this.cards[p], 0, 2);
            System.arraycopy(faceUp[p], 0, this.faceUp[p], 0, 2);
        }
        this.lastBattle = lastBattle;
        this.lines = lines;
    }
}
