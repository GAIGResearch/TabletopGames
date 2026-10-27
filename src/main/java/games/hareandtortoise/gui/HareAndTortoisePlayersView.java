package games.hareandtortoise.gui;

import games.hareandtortoise.HareAndTortoiseGameState;

import javax.swing.*;
import java.awt.*;

import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

/**
 * A table of the players beside the board: each runner's square, carrots, lettuces and place in the race, and
 * anything pending for it. Below it, the hare cards: how many have never been drawn, and the card drawn last.
 */
public class HareAndTortoisePlayersView extends JComponent {

    static final int WIDTH = 330;
    static final int ROW_HEIGHT = 54;
    static final int HEADER_HEIGHT = 26;
    static final int HARE_HEIGHT = 62;

    private final String[] agentNames;
    private HareAndTortoiseGameState state;

    public HareAndTortoisePlayersView(String[] agentNames) {
        this.agentNames = agentNames;
        Dimension size = new Dimension(WIDTH, HEADER_HEIGHT + agentNames.length * ROW_HEIGHT + HARE_HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    public void update(HareAndTortoiseGameState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (state == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(250, 246, 230));
        g2.fillRoundRect(0, 0, WIDTH - 1, getPreferredSize().height - 1, 12, 12);
        g2.setColor(Color.DARK_GRAY);
        g2.drawRoundRect(0, 0, WIDTH - 1, getPreferredSize().height - 1, 12, 12);

        Font plain = getFont().deriveFont(Font.PLAIN, 13f);
        Font bold = getFont().deriveFont(Font.BOLD, 13f);
        g2.setFont(bold);
        g2.drawString("Runners", 10, 19);

        int current = state.isNotTerminal() ? state.getCurrentPlayer() : -1;
        for (int p = 0; p < state.getNPlayers(); p++) {
            int top = HEADER_HEIGHT + p * ROW_HEIGHT;
            if (p == current) {
                g2.setColor(new Color(255, 228, 150));
                g2.fillRect(2, top, WIDTH - 4, ROW_HEIGHT - 2);
            }
            int r = HareAndTortoiseBoardView.RUNNER_RADIUS;
            g2.setColor(HareAndTortoiseBoardView.RUNNER_COLOURS[p % HareAndTortoiseBoardView.RUNNER_COLOURS.length]);
            g2.fillOval(10, top + 8, 2 * r, 2 * r);
            g2.setColor(Color.BLACK);
            g2.drawOval(10, top + 8, 2 * r, 2 * r);
            g2.setFont(bold);
            g2.drawString(String.valueOf(p), 10 + r - 4, top + 8 + r + 5);

            g2.drawString("Player " + p + "  " + agentNames[p], 44, top + 17);
            g2.setFont(plain);
            int square = state.getSquare(p);
            String where = square == 0 ? "START" : square == HOME_SQUARE ? "HOME" : "Square " + square
                    + " (" + state.getSquareType(p).name().toLowerCase() + ")";
            g2.drawString(where + "   " + placeText(p), 44, top + 33);
            g2.drawString("Carrots " + state.getCarrots(p) + "   Lettuces " + state.getLettuces(p)
                    + statusText(p), 44, top + 48);
        }

        int top = HEADER_HEIGHT + state.getNPlayers() * ROW_HEIGHT + 6;
        g2.setColor(Color.BLACK);
        g2.setFont(bold);
        g2.drawString("Hare cards", 10, top + 12);
        g2.setFont(plain);
        int deckSize = state.getHareDeck().getSize();
        int unseen = state.getNUnseenHareCards();
        g2.drawString(unseen + " of " + deckSize + " never drawn", 10, top + 29);
        // a drawn card goes to the bottom of the pile, so the bottom card is the last one drawn
        String last = unseen == deckSize ? "none yet"
                : state.getHareDeck().get(deckSize - 1).toString();
        g2.drawString("Last drawn: " + last, 10, top + 46);
    }

    private String placeText(int player) {
        int place = state.getRacePosition(player);
        String suffix = place == 1 ? "st" : place == 2 ? "nd" : place == 3 ? "rd" : "th";
        return (state.isHome(player) ? "Finished " : "") + place + suffix;
    }

    private String statusText(int player) {
        if (state.hasLettuceToChew(player)) return "   - chews a lettuce next";
        if (state.missesNextTurn(player)) return "   - misses next turn";
        return "";
    }
}
