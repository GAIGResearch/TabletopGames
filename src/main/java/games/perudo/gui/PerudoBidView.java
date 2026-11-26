package games.perudo.gui;

import core.CoreConstants;
import games.perudo.PerudoGameState;

import javax.swing.*;
import java.awt.*;

/**
 * The centre of the table: the current bid with a die showing its face, who made it, the dice on the table, and
 * whether this is a Palifico round. At the end of the game it names the winner.
 */
public class PerudoBidView extends JComponent {

    static final int WIDTH = 420;
    static final int HEIGHT = 120;
    static final int DIE_SIZE = 44;

    String headline = "";
    String detail = "";
    String roundType = "";
    int bidQuantity;
    int bidFace;

    public PerudoBidView() {
        Dimension size = new Dimension(WIDTH, HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setOpaque(false);
    }

    public void update(PerudoGameState state) {
        bidFace = state.getBidFace();
        bidQuantity = state.getBidQuantity();
        detail = "Dice on the table: " + state.getTotalDice();
        roundType = state.isPalificoRound() ? "Palifico round: Pacos are not wild, and the face is fixed" : "";
        if (state.getGameStatus() == CoreConstants.GameResult.GAME_END) {
            headline = "Game over";
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (state.getPlayerResults()[p] == CoreConstants.GameResult.WIN_GAME)
                    headline = "Game over: Player " + p + " wins";
            }
            roundType = "";
        } else if (state.hasBid()) {
            headline = "Bid by Player " + state.getBidder() + ":";
        } else {
            headline = "Player " + state.getCurrentPlayer() + " to open the bidding";
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        FontMetrics fm = g2.getFontMetrics();
        int y = 10 + DIE_SIZE / 2 + fm.getAscent() / 2;
        g2.drawString(headline, 10, y);
        if (bidFace != 0 && !headline.startsWith("Game over")) {
            int x = 10 + fm.stringWidth(headline) + 12;
            String quantity = bidQuantity + " x";
            g2.drawString(quantity, x, y);
            PerudoDiceView.drawDie(g2, x + fm.stringWidth(quantity) + 8, 10, DIE_SIZE, bidFace);
        }
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
        g2.setColor(Color.white);
        g2.drawString(detail, 10, 10 + DIE_SIZE + 24);
        if (!roundType.isEmpty()) {
            g2.setColor(new Color(255, 220, 120));
            g2.drawString(roundType, 10, 10 + DIE_SIZE + 46);
        }
    }
}
