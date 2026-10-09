package games.rummy.gui;

import core.CoreConstants;
import core.components.FrenchCard;
import games.rummy.RummyGameState;
import games.rummy.RummyMeld;
import games.rummy.RummyParameters;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The middle of the table: the draw deck, the discard pile, the melds and the public information of the deal.
 */
public class RummyTableView extends JComponent {

    private static final int FAN = 16, GAP = 12;

    private final Dimension size;
    private int drawDeckSize;
    private FrenchCard topDiscard;
    private int discardSize;
    private final List<List<FrenchCard>> melds = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();

    public RummyTableView(int width, int height) {
        size = new Dimension(width, height);
        setOpaque(false);
    }

    public void update(RummyGameState state) {
        RummyParameters params = (RummyParameters) state.getGameParameters();
        drawDeckSize = state.getDrawDeck().getSize();
        discardSize = state.getDiscardPile().getSize();
        topDiscard = discardSize == 0 ? null : state.getDiscardPile().peek();
        melds.clear();
        for (RummyMeld meld : state.getMelds())
            melds.add(new ArrayList<>(meld.getComponents()));

        lines.clear();
        int current = state.getCurrentPlayer();
        if (!state.isNotTerminal()) {
            lines.add("Game over: " + result(state));
            // the hands are no longer hidden
            StringBuilder points = new StringBuilder("Points in hand:");
            for (int p = 0; p < state.getNPlayers(); p++)
                points.append("  P").append(p).append(" ").append(state.handPoints(p));
            lines.add(points.toString());
        } else if (state.getGamePhase() == RummyGameState.Phase.DRAW) {
            lines.add("Player " + current + " draws");
        } else {
            lines.add("Player " + current + " melds, lays off or discards");
            if (state.hasMeldedThisTurn())
                lines.add("Player " + current + " has melded this turn");
            if (state.getTakenCard() != null && state.getPlayerHand(current).contains(state.getTakenCard()))
                lines.add("Taken from the discard pile: " + state.getTakenCard() + " (not to be discarded)");
        }
        if (state.isNotTerminal())
            lines.add("Turn " + (state.getTurnCounter() + 1) + " of at most " + params.maxTurnsPerDeal + " this deal");
        if (params.targetScore > 0) {
            StringBuilder scores = new StringBuilder("Scores:");
            for (int p = 0; p < state.getNPlayers(); p++)
                scores.append("  P").append(p).append(" ").append(state.getPlayerScore(p));
            lines.add(scores.toString());
            lines.add("Deal " + (state.getRoundCounter() + 1) + ", playing to " + params.targetScore);
        }
        repaint();
    }

    private static String result(RummyGameState state) {
        List<String> winners = new ArrayList<>();
        for (int p = 0; p < state.getNPlayers(); p++) {
            CoreConstants.GameResult r = state.getPlayerResults()[p];
            if (r == CoreConstants.GameResult.WIN_GAME || r == CoreConstants.GameResult.DRAW_GAME)
                winners.add("Player " + p);
        }
        return winners.size() == 1 ? winners.get(0) + " wins" : String.join(" and ", winners) + " share first place";
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        int cw = RummyGUIManager.CARD_WIDTH, ch = RummyGUIManager.CARD_HEIGHT;
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));

        // the draw deck and the discard pile
        int x = 10, y = 10;
        if (drawDeckSize > 0)
            g.drawImage(RummyDeckView.backImage(), x, y, cw, ch, null);
        else
            g.drawRect(x, y, cw, ch);
        g.drawString("Draw deck " + drawDeckSize, x, y + ch + 16);
        x += cw + 90;
        if (topDiscard != null)
            g.drawImage(RummyDeckView.cardImage(topDiscard), x, y, cw, ch, null);
        else
            g.drawRect(x, y, cw, ch);
        g.drawString("Discard pile " + discardSize, x, y + ch + 16);

        int ly = y + 14;
        for (String line : lines) {
            g.drawString(line, x + cw + 70, ly);
            ly += 18;
        }

        // the melds, in the order they were laid down, wrapping onto new rows
        int top = Math.max(y + ch + 44, ly + 10);
        g.drawString(melds.isEmpty() ? "No melds yet" : "Melds", 10, top);
        x = 10;
        y = top + 8;
        for (List<FrenchCard> meld : melds) {
            int w = cw + FAN * (meld.size() - 1);
            if (x + w > size.width - 10 && x > 10) {
                x = 10;
                y += ch + GAP;
            }
            for (int i = 0; i < meld.size(); i++)
                g.drawImage(RummyDeckView.cardImage(meld.get(i)), x + i * FAN, y, cw, ch, null);
            x += w + GAP;
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
