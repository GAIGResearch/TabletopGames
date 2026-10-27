package games.lawnandorder.gui;

import games.lawnandorder.components.RuleCard;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The Rule cards revealed this round, the Insider Tips and the round's status line.
 */
public class LawnAndOrderTableView extends JComponent {

    static final int ruleWidth = 88, ruleHeight = 40, gap = 6;
    final int width, height;
    // the revealed Rule cards wrap onto as many rows as the most that can be revealed in a round needs
    final int perRow, revealedRows;

    String status = "";
    List<RuleCard> revealed = new ArrayList<>();
    List<RuleCard> tips = new ArrayList<>();
    List<Boolean> tipsVisible = new ArrayList<>();
    int nPlayers;

    /**
     * @param maxRevealed the most Rule cards that can be revealed in a round
     */
    public LawnAndOrderTableView(int width, int maxRevealed) {
        this.width = width;
        perRow = Math.max(1, (width - 16 + gap) / (ruleWidth + gap));
        revealedRows = Math.max(1, (maxRevealed + perRow - 1) / perRow);
        height = 20 + 16 + revealedRows * (ruleHeight + gap) + 14 + ruleHeight + 10;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.drawString(status, 8, 17);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        int y = 20 + 14;
        g2.drawString("Revealed this round (in force until the round ends)", 8, y - 2);
        if (revealed.isEmpty())
            g2.drawString("-", 10, y + ruleHeight / 2);
        // in the order they were revealed, the first on the left
        for (int i = 0; i < revealed.size(); i++)
            LawnCardArt.drawRule(g2, revealed.get(revealed.size() - 1 - i), new Rectangle(
                    8 + (i % perRow) * (ruleWidth + gap), y + (i / perRow) * (ruleHeight + gap), ruleWidth, ruleHeight), true);

        y += revealedRows * (ruleHeight + gap) + 12;
        g2.setColor(Color.white);
        g2.drawString("Insider Tips (each seen by the two players beside it)", 8, y - 2);
        int x = 8;
        for (int i = 0; i < tips.size(); i++) {
            Rectangle r = new Rectangle(x, y, ruleWidth, ruleHeight);
            LawnCardArt.drawRule(g2, tips.get(i), r, tipsVisible.get(i));
            g2.setColor(Color.white);
            String between = "P" + i + "|P" + (i + 1) % nPlayers;
            g2.drawString(between, x + ruleWidth + 3, y + ruleHeight - 4);
            x += ruleWidth + 3 + g2.getFontMetrics().stringWidth(between) + 12;
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
     * @param tipsVisible for each Insider Tip, whether the viewer may see it
     */
    public void update(String status, List<RuleCard> revealed, List<RuleCard> tips, List<Boolean> tipsVisible,
                       int nPlayers) {
        this.status = status;
        this.revealed = new ArrayList<>(revealed);
        this.tips = new ArrayList<>(tips);
        this.tipsVisible = new ArrayList<>(tipsVisible);
        this.nPlayers = nPlayers;
    }
}
