package games.president.gui;

import core.CoreConstants;
import core.components.FrenchCard;
import games.president.PresidentGameState;
import games.president.PresidentParameters;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The middle of the table: the sets played to the current trick, oldest on the left, and the public information of
 * the deal.
 */
public class PresidentTableView extends JComponent {

    // the most recent sets shown
    private static final int MAX_SETS = 5;

    private final Dimension size;
    private final List<List<FrenchCard>> sets = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();

    public PresidentTableView(int width, int height) {
        size = new Dimension(width, height);
        setOpaque(false);
    }

    public void update(PresidentGameState state) {
        PresidentParameters params = (PresidentParameters) state.getGameParameters();
        sets.clear();
        // the play pile has the latest set on top (index 0), and every set of a trick has setSize cards
        List<FrenchCard> pile = state.getPlayPile().getComponents();
        int setSize = state.getSetSize();
        if (setSize > 0) {
            for (int start = 0; start < pile.size() && sets.size() < MAX_SETS; start += setSize)
                sets.add(0, new ArrayList<>(pile.subList(start, Math.min(start + setSize, pile.size()))));
        }
        lines.clear();
        if (!state.isNotTerminal()) {
            lines.add("Game over: Player " + winner(state) + " wins");
        } else if (state.getGamePhase() == PresidentGameState.Phase.EXCHANGE) {
            lines.add("Player " + state.getCurrentPlayer() + " (President) gives " + state.getCardsToGive()
                    + " more card(s) to Player " + state.getScum() + " (Scum)");
        } else if (setSize == 0) {
            lines.add("Player " + state.getCurrentPlayer() + " leads any set");
        } else {
            lines.add("To beat: " + setSize + " card(s) higher than Player " + state.getLastPlayer() + "'s set");
            lines.add("Passes since that set: " + state.getPassesInRow());
        }
        if (state.isNotTerminal() && state.getGamePhase() == PresidentGameState.Phase.PLAY)
            lines.add("Sets played this trick: " + (setSize == 0 ? 0 : pile.size() / setSize)
                    + "   Discard pile: " + state.getDiscardPile().getSize() + " cards");
        List<Integer> order = state.getFinishingOrder();
        if (!order.isEmpty())
            lines.add("Out this deal: " + order.stream().map(p -> "P" + p).reduce((a, b) -> a + ", " + b).orElse(""));
        StringBuilder scores = new StringBuilder("Scores:");
        for (int p = 0; p < state.getNPlayers(); p++)
            scores.append("  P").append(p).append(" ").append(state.getPlayerScore(p));
        lines.add(scores.toString());
        if (params.targetScore > 1)
            lines.add("Deal " + (state.getRoundCounter() + 1) + ", playing to " + params.targetScore);
        repaint();
    }

    private static int winner(PresidentGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.getPlayerResults()[p] == CoreConstants.GameResult.WIN_GAME) return p;
        return -1;
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        int cw = PresidentGUIManager.CARD_WIDTH, ch = PresidentGUIManager.CARD_HEIGHT;
        int x = 10, y = 10;
        for (int s = 0; s < sets.size(); s++) {
            List<FrenchCard> set = sets.get(s);
            // earlier sets are dimmed, the set to beat is at full strength
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, s == sets.size() - 1 ? 1f : 0.55f));
            for (int i = 0; i < set.size(); i++)
                g.drawImage(PresidentDeckView.cardImage(set.get(i)), x + i * 16, y, cw, ch, null);
            x += cw + 16 * (set.size() - 1) + 10;
        }
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
        int ly = y + ch + 25;
        for (String line : lines) {
            g.drawString(line, 10, ly);
            ly += 18;
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
