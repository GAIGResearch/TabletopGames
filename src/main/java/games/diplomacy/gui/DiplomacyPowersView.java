package games.diplomacy.gui;

import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyPhase;

import javax.swing.*;
import java.awt.*;

/**
 * The season and phase, and a row for each power: its colour, name and player, supply centres and units, and what
 * it is doing this phase (ordering, done, or in the adjustment phase its builds or disbands). Powers out of the game
 * are greyed.
 */
public class DiplomacyPowersView extends JComponent {

    static final int WIDTH = 280, ROW = 34, TOP = 44;

    final String[] agentNames;
    DiplomacyGameState state;

    public DiplomacyPowersView(String[] agentNames) {
        this.agentNames = agentNames;
    }

    public void update(DiplomacyGameState state) {
        this.state = state;
        repaint();
    }

    static String phaseName(DiplomacyGameState state) {
        DiplomacyPhase phase = state.getPhase();
        String season = switch (phase) {
            case SPRING_ORDERS, SPRING_RETREATS -> "Spring";
            case FALL_ORDERS, FALL_RETREATS -> "Fall";
            case ADJUSTMENTS -> "Winter";
        };
        String step = phase.isOrders() ? "orders" : phase.isRetreats() ? "retreats" : "builds and disbands";
        return season + " " + state.getYear() + " - " + step;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 241, 230));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (state == null) return;

        g.setColor(Color.black);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString(state.isNotTerminal() ? phaseName(state) : "Game over, " + state.getYear(), 10, 22);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(state.getMap().victoryCentres() + " centres win", 10, 38);

        for (int p = 0; p < state.getNPlayers(); p++) {
            int y = TOP + p * ROW;
            boolean out = state.nUnits(p) == 0 && state.nCentres(p) == 0;
            boolean current = state.isNotTerminal() && state.getCurrentPlayer() == p;
            if (current) {
                g.setColor(new Color(255, 236, 160));
                g.fillRect(2, y, WIDTH - 4, ROW - 2);
            }
            Color colour = DiplomacyMapView.powerColour(p);
            g.setColor(out ? Color.lightGray : colour);
            g.fillRoundRect(8, y + 6, 20, 20, 6, 6);
            g.setColor(Color.black);
            g.drawRoundRect(8, y + 6, 20, 20, 6, 6);

            g.setColor(out ? Color.gray : Color.black);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(state.getMap().powers().get(p), 36, y + 15);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g.drawString(agentNames[p], 36, y + 28);

            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(state.nCentres(p) + " centres, " + state.nUnits(p) + " units", 128, y + 15);
            g.setFont(new Font("SansSerif", Font.ITALIC, 11));
            g.drawString(status(p, current), 128, y + 28);
        }
    }

    private String status(int power, boolean current) {
        if (!state.isNotTerminal())
            return switch (state.getPlayerResults()[power]) {
                case WIN_GAME -> "wins";
                case DRAW_GAME -> "shares the win";
                default -> "";
            };
        if (current)
            return "to order";
        if (state.getPhase() == DiplomacyPhase.ADJUSTMENTS) {
            int adjustment = state.adjustment(power);
            if (adjustment > 0)
                return state.hasOrdersToGive(power) ? adjustment + " to build" : "";
            if (adjustment < 0)
                return -adjustment + " to disband";
            return "";
        }
        return state.hasOrdersToGive(power) ? "to order" : "";
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, TOP + agentNames.length * ROW + 6);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
