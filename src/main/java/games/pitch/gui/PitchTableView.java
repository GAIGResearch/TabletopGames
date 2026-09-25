package games.pitch.gui;

import core.CoreConstants;
import core.components.FrenchCard;
import games.pitch.PitchGameState;
import games.pitch.PitchParameters;
import games.pitch.PitchUtils;
import games.tricktaking.Trick;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The middle of the table: the current trick, each card in front of the player who played it, and the deal's
 * public information (trumps, the pitcher and their bid, tricks won and the team scores).
 */
public class PitchTableView extends JComponent {

    private final Dimension size;
    private Trick trick;
    private final List<String> lines = new ArrayList<>();

    public PitchTableView(int width, int height) {
        size = new Dimension(width, height);
        setOpaque(false);
    }

    public void update(PitchGameState state) {
        trick = state.getCurrentTrick().copy();
        lines.clear();
        FrenchCard.Suite trumps = state.getTrumpSuit();
        lines.add("Trumps: " + (trumps == null ? "not yet set" : trumps));
        int pitcher = state.getPitcher();
        lines.add(pitcher < 0 ? "No bid yet" : "Pitcher: Player " + pitcher + ", bid " + state.getHighestBid());
        for (int team = 0; team < 2; team++)
            lines.add(String.format("Team %d (P%d+P%d): score %d, tricks %d", team, team, team + 2,
                    state.getTeamScore(team), state.getTricksWon(team)));
        if (!state.isNotTerminal()) {
            // the cards of the last deal are still in the team decks
            int[] points = PitchUtils.teamPoints(List.of(state.getTeamTricks(0), state.getTeamTricks(1)), trumps,
                    (PitchParameters) state.getGameParameters());
            lines.add(String.format("Points in the last deal: team 0 %d, team 1 %d", points[0], points[1]));
            CoreConstants.GameResult result = state.getPlayerResults()[0];
            lines.add("Game over: " + (result == CoreConstants.GameResult.DRAW_GAME ? "a draw"
                    : "team " + (result == CoreConstants.GameResult.WIN_GAME ? 0 : 1) + " wins"));
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        int cw = PitchGUIManager.CARD_WIDTH, ch = PitchGUIManager.CARD_HEIGHT;
        int cx = size.width / 2 - cw / 2, cy = 10 + ch / 2 + 10;
        for (int i = 0; trick != null && i < trick.getSize(); i++) {
            // player 0 at the bottom, then clockwise: 1 left, 2 top, 3 right
            Point p = switch (trick.playerOf(i)) {
                case 0 -> new Point(cx, cy + ch / 2 + 5);
                case 1 -> new Point(cx - cw - 10, cy);
                case 2 -> new Point(cx, cy - ch / 2 - 5);
                default -> new Point(cx + cw + 10, cy);
            };
            g.drawImage(PitchDeckView.cardImage(trick.get(i)), p.x, p.y, cw, ch, null);
        }
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
        int y = cy + ch + ch / 2 + 30;
        for (String line : lines) {
            g.drawString(line, 10, y);
            y += 18;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return size;
    }
}
