package games.perudo.gui;

import games.perudo.PerudoGameState;

import javax.swing.*;
import java.awt.*;

/**
 * One player's area: their dice (face up only when the viewer may see them, sorted by face) and how many they hold,
 * or when they went out.
 */
public class PerudoPlayerView extends JComponent {

    static final int DIE_SIZE = 34;
    static final int GAP = 6;
    // one row holds the largest tunable PerudoParameters.nDicePerPlayer, and a Calza cannot go above it
    static final int DICE_PER_ROW = 6;
    static final int AREA_WIDTH = DICE_PER_ROW * (DIE_SIZE + GAP) + GAP;
    static final int AREA_HEIGHT = DIE_SIZE + GAP + 30;

    final int player;
    int[] dice = new int[0];
    boolean faceUp;
    String status = "";

    public PerudoPlayerView(int player) {
        this.player = player;
        Dimension size = new Dimension(AREA_WIDTH, AREA_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setOpaque(false);
    }

    public void update(PerudoGameState state, boolean faceUp) {
        this.dice = state.getDice(player);
        this.faceUp = faceUp;
        int nDice = state.getNDice(player);
        if (state.getEliminationRound(player) >= 0)
            status = "Out in round " + state.getEliminationRound(player);
        else
            status = nDice + (nDice == 1 ? " die" : " dice") + (state.hasHadPalifico(player) ? " - has had Palifico" : "");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int index = 0;
        for (int f = 0; f < dice.length; f++) {
            for (int d = 0; d < dice[f]; d++) {
                int x = GAP + (index % DICE_PER_ROW) * (DIE_SIZE + GAP);
                int y = GAP + (index / DICE_PER_ROW) * (DIE_SIZE + GAP);
                if (faceUp)
                    PerudoDiceView.drawDie(g2, x, y, DIE_SIZE, f + 1);
                else
                    PerudoDiceView.drawHiddenDie(g2, x, y, DIE_SIZE);
                index++;
            }
        }
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
        g2.drawString(status, GAP, AREA_HEIGHT - 8);
    }
}
