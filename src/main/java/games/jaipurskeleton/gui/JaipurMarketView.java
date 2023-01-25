package games.jaipurskeleton.gui;

import games.jaipurskeleton.JaipurGUIManager;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.components.JaipurCard;

import javax.swing.*;
import java.awt.*;

import static games.jaipurskeleton.JaipurGUIManager.viewHeight;
import static games.jaipurskeleton.JaipurGUIManager.viewWidth;
import static gui.AbstractGUIManager.defaultItemSize;

public class JaipurMarketView extends JComponent {
    JaipurGameState gs;
    int offset = 10;
    Dimension size;

    public JaipurMarketView(JaipurGameState gs) {
        this.gs = gs;
        this.size = new Dimension(viewWidth, viewHeight);
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;
        int fontSize = g.getFont().getSize();

        g.setColor(Color.black);
        g.drawString("MARKET", 0, fontSize);

        int yGap = fontSize*2 + offset;
        int y = 0;
        for (JaipurCard.GoodType gt: JaipurCard.GoodType.values()) {
            int inMarket = gs.getMarket().get(gt).getValue();
            for (int i = 0; i < inMarket; i++) {
                g.setColor(JaipurGUIManager.goodColorMapping.get(gt));
                g.fillRect(0, yGap+y * offset, defaultItemSize, defaultItemSize);
                g.setColor(Color.black);
                g.drawRect(0, yGap+y * offset, defaultItemSize, defaultItemSize);
                g.setColor(Color.white);
                g.drawString(gt.name(), 2, yGap+y * offset + offset);
                y++;
            }
        }

        // draw n cards left in draw pile
        g.setColor(Color.black);
        g.drawString("Draw pile: " + gs.getDrawDeck().getSize(), 0, yGap*2 + y * offset + defaultItemSize);
    }

    public void update(JaipurGameState gs) {
        this.gs = gs;
    }
    @Override
    public Dimension getPreferredSize() {
        return size;
    }
}
