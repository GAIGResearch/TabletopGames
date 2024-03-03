package games.jaipurskeleton.gui;

import core.components.Deck;
import games.jaipurskeleton.JaipurGUIManager;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.components.JaipurCard;
import games.jaipurskeleton.components.JaipurToken;
import games.jaipurskeleton.JaipurParameters;

import javax.swing.*;
import java.awt.*;

import static gui.AbstractGUIManager.defaultItemSize;

public class JaipurGoodTokenView extends JComponent {
    JaipurGameState gs;
    Dimension size;
    int offset = 10;
    Color fColor = new Color(0,0,0, 80);
    Color outColor = new Color(238, 13, 13, 80);

    public JaipurGoodTokenView(JaipurGameState gs) {
        this.gs = gs;
        this.size = new Dimension(nMaxGoodTokensLength(gs)*(offset+1) + (defaultItemSize-offset), (JaipurCard.GoodType.values().length-1) * defaultItemSize);
    }

    private int nMaxGoodTokensLength(JaipurGameState gs) {
        return ((JaipurParameters) gs.getGameParameters()).getGoodTokensProgression().values().stream().mapToInt(p -> p.length).max().orElse(0);
        //return 9;
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;

        // Display each stack of good tokens, showing the numbers left on each
        int y = 0;

        for (JaipurCard.GoodType gt: JaipurCard.GoodType.values()) {
            Deck<JaipurToken> deck = gs.getGoodTokens().get(gt);
            if (deck == null) {
                continue;
            }
            Integer[] progression = ((JaipurParameters)gs.getGameParameters()).getGoodTokensProgression().get(gt);
            drawTokens(g, progression, deck,y, JaipurGUIManager.soldGoodColorMapping.get(gt), outColor, fColor, true);
            drawTokens(g, progression, deck, y, JaipurGUIManager.goodColorMapping.get(gt), Color.black, Color.white, false);
            y += defaultItemSize;
        }
    }

    private void drawTokens(Graphics2D g, Integer[] progression, Deck<JaipurToken> deck, int y,
                            Color tokenColor, Color outlineColor, Color fontColor, boolean sold) {
        if (sold) {
            for (int i = progression.length - 1; i >= deck.getSize(); i--) {
                g.setColor(tokenColor);
                g.fillOval(i * offset, y, defaultItemSize, defaultItemSize);
                g.setColor(outlineColor);
                g.drawOval(i * offset, y, defaultItemSize, defaultItemSize);
                g.setColor(fontColor);
                g.drawString("" + progression[i], (i - 1) * offset + offset / 4 + defaultItemSize, y + defaultItemSize / 2);
            }
        } else {
            for (int i = 0; i < deck.getSize(); i++) {
                g.setColor(tokenColor);
                g.fillOval(i * offset, y, defaultItemSize, defaultItemSize);
                g.setColor(outlineColor);
                g.drawOval(i * offset, y, defaultItemSize, defaultItemSize);
                g.setColor(fontColor);
                g.drawString("" + progression[i], i * offset + offset / 3, y + defaultItemSize / 2);
            }
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
