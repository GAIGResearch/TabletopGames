package games.jaipurskeleton.gui;

import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.JaipurParameters;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;

import static games.jaipurskeleton.JaipurGUIManager.viewWidth;

public class JaipurBonusTokenView extends JComponent {
    JaipurGameState gs;
    Dimension size;
    int offset = 20;

    public JaipurBonusTokenView(JaipurGameState gs) {
        this.gs = gs;
        this.size = new Dimension(viewWidth*(2+gs.getNPlayers()), 50);
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;
        int fontSize = g.getFont().getSize();
        FontMetrics fm = g.getFontMetrics();

        // Display bonus tokens, showing how many remain in each stack and the number of cards needed to sell to get one of those
        g.setColor(Color.black);
        g.drawString("BONUS", 0, fontSize);
        g.drawString("TOKENS", 0, fontSize*2);

        Map<Integer, Integer[]> bonusTokensAvailable = ((JaipurParameters)gs.getGameParameters()).getBonusTokensAvailable();
        int i = 0;
        int padding = fm.stringWidth("BONUS:") + offset;
        for (int minSell: bonusTokensAvailable.keySet()) {
            String line1 = "Minimum sell: " + minSell;
            String line2 = "Bonus " + i + " #left: " + gs.getBonusTokens().get(minSell).getSize() + "/" + bonusTokensAvailable.get(minSell).length;
            String line3 = "Values: " + Arrays.toString(bonusTokensAvailable.get(minSell));
            g.drawString(line1, i*offset + padding, fontSize);
            g.drawString(line2, i*offset + padding, fontSize*2);
            g.drawString(line3, i*offset + padding, fontSize*3);
            i++;

            int maxWidth = 0;
            if (fm.stringWidth(line1) > maxWidth) maxWidth = fm.stringWidth(line1);
            if (fm.stringWidth(line2) > maxWidth) maxWidth = fm.stringWidth(line2);
            if (fm.stringWidth(line3) > maxWidth) maxWidth = fm.stringWidth(line3);
            padding += maxWidth;
        }
    }

    public void update(JaipurGameState gs) {
        this.gs = gs;
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }
}
