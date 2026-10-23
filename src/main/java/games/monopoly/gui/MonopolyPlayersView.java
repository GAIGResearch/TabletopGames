package games.monopoly.gui;

import games.monopoly.MonopolyGameState;

import javax.swing.*;
import java.awt.*;

/**
 * A table of the players beside the board: each player's cash, total worth, properties and square, whether they are
 * in Jail, the Get Out of Jail Free cards they hold, and the place of each bankrupt player.
 */
public class MonopolyPlayersView extends JComponent {

    static final int WIDTH = 340;
    static final int ROW_HEIGHT = 50;
    static final int HEADER_HEIGHT = 26;

    private final String[] agentNames;
    private MonopolyGameState state;

    public MonopolyPlayersView(String[] agentNames) {
        this.agentNames = agentNames;
        Dimension size = new Dimension(WIDTH, HEADER_HEIGHT + agentNames.length * ROW_HEIGHT + 4);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    public void update(MonopolyGameState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (state == null) return;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int height = getPreferredSize().height;
        g2.setColor(new Color(250, 246, 230));
        g2.fillRoundRect(0, 0, WIDTH - 1, height - 1, 12, 12);
        g2.setColor(Color.DARK_GRAY);
        g2.drawRoundRect(0, 0, WIDTH - 1, height - 1, 12, 12);

        Font plain = getFont().deriveFont(Font.PLAIN, 12f);
        Font bold = getFont().deriveFont(Font.BOLD, 12f);
        g2.setFont(bold);
        g2.drawString("Players", 10, 18);
        String currency = state.getBoard().currency();

        int current = state.isNotTerminal() ? state.getCurrentPlayer() : -1;
        for (int p = 0; p < state.getNPlayers(); p++) {
            int top = HEADER_HEIGHT + p * ROW_HEIGHT;
            if (p == current) {
                g2.setColor(new Color(255, 228, 150));
                g2.fillRect(2, top, WIDTH - 4, ROW_HEIGHT - 2);
            }
            int t = MonopolyBoardView.TOKEN + 4;
            g2.setColor(MonopolyBoardView.playerColour(p));
            g2.fillOval(8, top + 6, t, t);
            g2.setColor(Color.BLACK);
            g2.drawOval(8, top + 6, t, t);
            g2.setFont(bold);
            g2.setColor(p == 2 ? Color.BLACK : Color.WHITE);
            g2.drawString(String.valueOf(p), 8 + t / 2 - 3, top + 6 + t / 2 + 5);

            g2.setColor(Color.BLACK);
            g2.drawString("Player " + p + "  " + agentNames[p], 38, top + 15);
            g2.setFont(plain);
            if (state.isBankrupt(p)) {
                g2.drawString("Bankrupt - finished " + ordinal(state.getFinalPlace(p)), 38, top + 31);
                continue;
            }
            g2.drawString("Cash " + currency + state.getCash(p) + "   Worth " + currency + state.getNetWorth(p)
                    + "   Properties " + state.getProperties(p).size(), 38, top + 30);
            int fails = state.getJailRolls(p);
            String where = state.isInJail(p) ? "In Jail (" + fails + " failed roll" + (fails == 1 ? ")" : "s)")
                    : MonopolyBoardView.label(state.getPosition(p));
            int cards = state.getJailCards(p).getSize();
            if (cards > 0)
                where += "   Get Out of Jail Free " + cards;
            g2.drawString(where, 38, top + 44);
        }
    }

    static String ordinal(int place) {
        String suffix = place == 1 ? "st" : place == 2 ? "nd" : place == 3 ? "rd" : "th";
        return place + suffix;
    }
}
