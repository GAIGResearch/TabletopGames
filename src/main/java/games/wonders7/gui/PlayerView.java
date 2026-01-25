package games.wonders7.gui;

import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PlayerView extends JComponent {
    Wonders7GameState gs;
    int playerId;
    static int width = 3000;
    static int height = 300;
    Dimension size = new Dimension(width, height);
    int pad = 2;
    int borderHeight = 20;

    public PlayerView(Wonders7GameState gs, int playerId) {
        this.gs = gs;
        this.playerId = playerId;
    }

    @Override
    protected void paintComponent(Graphics g) {
        /*
            - score
            - resources: List<HashMap<Wonder7Card.resources, Integer>> playerResources
            - cards to choose from:  List<Deck<Wonder7Card>> playerHands;
            - cards played:  List<Deck<Wonder7Card>> playedCards;
            - Wonder board
         */
        int fontSize = g.getFont().getSize();
        Map<Wonders7Constants.Resource, Integer> playerResources = gs.getPlayerResources(playerId);

        g.drawRect(pad,pad,width-pad*2, height-pad*2-borderHeight);
        int y = pad*2 + fontSize;
        g.drawString("Score: " + gs.getGameScore(playerId), pad*2, y);
        for (Wonders7Constants.Resource res: playerResources.keySet()) {
            y += fontSize;
            g.drawString(res.name() + ": " + playerResources.get(res), pad*2, y);
        }
        // cards played: TODO
        // wonder:
        y += fontSize*2;
        g.drawString("Wonder: " + gs.getPlayerWonderBoard(playerId).toString(), pad*2, y);
        // cards to choose from:
        y += fontSize*2;
        g.drawString("Cards in hand: " + gs.getPlayerHand(playerId).toString(), pad*2, y);
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }
}
