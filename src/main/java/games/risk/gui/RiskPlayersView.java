package games.risk.gui;

import games.risk.RiskGameState;
import games.risk.RiskParameters;
import games.risk.components.RiskCard;
import games.risk.components.RiskMission;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntPredicate;

/**
 * One row per player: their colour, territories and armies, their RISK cards and Secret Mission (face up only to a
 * viewer who may see them), and whether they are out. Below, the state of the turn and of the cards.
 */
public class RiskPlayersView extends JComponent {

    static final Color[] PLAYER_COLOURS = {new Color(200, 40, 40), new Color(40, 80, 200), new Color(40, 150, 60),
            new Color(235, 200, 30), new Color(130, 60, 160), new Color(60, 60, 60)};
    static final int WIDTH = 310, LINE = 15;

    final String[] agentNames;
    final IntPredicate visible;
    RiskGameState state;

    /**
     * @param visible whether a player's cards and mission may be shown face up to whoever is watching
     */
    public RiskPlayersView(String[] agentNames, IntPredicate visible) {
        this.agentNames = agentNames;
        this.visible = visible;
    }

    public void update(RiskGameState state) {
        this.state = state;
    }

    /** Black or white, whichever reads better on the colour. */
    static Color textOn(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 150 ? Color.black : Color.white;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 242, 232));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (state == null) return;
        RiskParameters params = (RiskParameters) state.getGameParameters();
        Font bold = new Font("SansSerif", Font.BOLD, 12);
        Font plain = new Font("SansSerif", Font.PLAIN, 11);

        int y = 4;
        for (int p = 0; p < state.getNPlayers(); p++) {
            boolean current = p == state.getCurrentPlayer() && state.isNotTerminal();
            if (current) {
                g.setColor(new Color(255, 250, 200));
                g.fillRect(0, y, WIDTH, rowHeight() - 4);
            }
            g.setColor(PLAYER_COLOURS[p % PLAYER_COLOURS.length]);
            g.fillRect(6, y + 4, 16, 16);
            g.setColor(Color.black);
            g.setFont(bold);
            g.drawString((current ? "> " : "") + "Player " + p + " [" + agentNames[p] + "]", 28, y + 17);
            g.setFont(plain);
            String status = state.isEliminated(p)
                    ? "Out (place " + state.getFinalPlace(p) + ")"
                    : state.getNTerritories(p) + " territories, " + state.getTotalArmies(p) + " armies";
            if (!state.isNotTerminal())
                status += " - " + state.getPlayerResults()[p];
            g.drawString(status, 28, y + 33);
            g.drawString("Cards: " + cards(p), 28, y + 48);
            RiskMission mission = state.getMission(p);
            if (mission != null)
                drawWrapped(g, "Mission: " + (!visible.test(p) ? "hidden" : mission.isBackup(state, p)
                        ? "Occupy " + state.getMap().backupTerritories() + " territories (for " + mission + ")"
                        : mission), 28, y + 63);
            y += rowHeight();
        }

        g.setColor(Color.black);
        g.setFont(bold);
        g.drawString("Turn", 6, y + 14);
        g.setFont(plain);
        String turn = "Step: " + state.getGamePhase() + ", round " + (state.getRoundCounter() + 1) + " of "
                + params.getMaxRounds();
        g.drawString(state.isNotTerminal() ? turn : "Game over", 6, y + 30);
        g.drawString("Armies to place: " + state.getArmiesToPlace(state.getCurrentPlayer()), 6, y + 45);
        g.drawString("Sets traded: " + state.getNSetsTraded() + "; the next is worth "
                + params.tradeValue(state.getNSetsTraded() + 1), 6, y + 60);
        g.drawString("Draw deck " + state.getDrawDeck().getSize() + ", discard pile "
                + state.getDiscardPile().getSize(), 6, y + 75);
    }

    /** A row has a fourth line, which may wrap to a fifth, for the Secret Mission. */
    private int rowHeight() {
        boolean missions = state != null && ((RiskParameters) state.getGameParameters()).secretMission;
        return missions ? 4 + 5 * LINE + 9 : 4 + 3 * LINE + 11;
    }

    /** The text on one line, or on two if it is wider than the panel (split at a space). */
    private static void drawWrapped(Graphics2D g, String text, int x, int baseline) {
        FontMetrics fm = g.getFontMetrics();
        int room = WIDTH - x - 6;
        if (fm.stringWidth(text) <= room) {
            g.drawString(text, x, baseline);
            return;
        }
        int split = text.lastIndexOf(' ');
        while (split > 0 && fm.stringWidth(text.substring(0, split)) > room)
            split = text.lastIndexOf(' ', split - 1);
        g.drawString(text.substring(0, split), x, baseline);
        g.drawString(text.substring(split + 1), x + 10, baseline + LINE);
    }

    /**
     * The player's RISK cards by symbol (I Infantry, C Cavalry, A Artillery, W wild), or just how many if they cannot
     * be seen.
     */
    private String cards(int player) {
        int n = state.getHand(player).getSize();
        if (n == 0) return "none";
        if (!visible.test(player)) return n + " (hidden)";
        StringBuilder sb = new StringBuilder();
        for (RiskCard c : state.getHand(player).getComponents())
            sb.append(c.symbol.name().charAt(0)).append(' ');
        return sb.toString().trim();
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = state == null ? 6 : state.getNPlayers();
        return new Dimension(WIDTH, rows * rowHeight() + 90);
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
