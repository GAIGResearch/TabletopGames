package games.jaipurskeleton.gui;

import core.AbstractPlayer;
import games.jaipurskeleton.JaipurGUIManager;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.components.JaipurCard;

import javax.swing.*;
import java.awt.*;

import static games.jaipurskeleton.JaipurGUIManager.viewHeight;
import static games.jaipurskeleton.JaipurGUIManager.viewWidth;
import static games.jaipurskeleton.components.JaipurCard.GoodType.Camel;
import static gui.AbstractGUIManager.defaultItemSize;

public class JaipurPlayerArea extends JComponent {
    int playerId;
    JaipurGameState gs;
    AbstractPlayer player;
    Dimension size;
    int offset = 10;

    public JaipurPlayerArea(JaipurGameState gs, AbstractPlayer player, int playerId) {
        this.gs = gs;
        this.playerId = playerId;
        this.player = player;
        this.size = new Dimension(viewWidth, viewHeight);
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;
        int fontSize = g.getFont().getSize();

        // Draw player name
        g.setColor(Color.black);
        g.drawString("PLAYER " + playerId, 0, fontSize);
        g.drawString(player.toString(), 0, fontSize*2);

        // Draw cards in hand
        int gap = fontSize*2 + offset;
        int yGap = gap;
        int y = 0;
        for (JaipurCard.GoodType gt: JaipurCard.GoodType.values()) {
            if (gt == Camel) continue;
            int inHand = gs.getPlayerHands().get(playerId).get(gt).getValue();
            for (int i = 0; i < inHand; i++) {
                g.setColor(JaipurGUIManager.goodColorMapping.get(gt));
                g.fillRect(0, yGap+y * offset, defaultItemSize, defaultItemSize);
                g.setColor(Color.black);
                g.drawRect(0, yGap+y * offset, defaultItemSize, defaultItemSize);
                g.setColor(Color.white);
                g.drawString(gt.name(), 2, yGap+y * offset + offset);
                y++;
            }
        }

        // Draw n camels
        yGap = gap + offset + defaultItemSize;
        int camels = gs.getPlayerHerds().get(playerId).getValue();
        for (int i = 0; i < camels; i++) {
            g.setColor(JaipurGUIManager.goodColorMapping.get(Camel));
            g.fillRect(0, yGap+y * offset, defaultItemSize, defaultItemSize);
            g.setColor(Color.black);
            g.drawRect(0, yGap+y * offset, defaultItemSize, defaultItemSize);
            g.setColor(Color.white);
            g.drawString(Camel.name(), 2, yGap+y * offset + offset);
            y++;
        }

        // Draw player score
        yGap = gap + offset*2 + defaultItemSize;
        g.setColor(Color.black);
        g.drawString("Score: " + gs.getPlayerScores().get(playerId), 0, yGap + y * offset + defaultItemSize);
        g.drawString("Rounds win: " + gs.getPlayerNRoundsWon().get(playerId), 0, yGap + y * offset + defaultItemSize + fontSize);
    }

    public void update(JaipurGameState gs) {
        this.gs = gs;
    }
    @Override
    public Dimension getPreferredSize() {
        return size;
    }
}
