package games.cribbage.gui;

import core.components.FrenchCard;
import games.cribbage.CribbageGameState;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static games.cribbage.gui.CribbageGUIManager.*;

/**
 * The centre of the table: the starter card (face-down until it is turned up), the crib (each card face-up only to
 * a human viewer who discarded it), and the cards of the current count in the order played, with the running total.
 */
public class CribbageTableView extends JComponent {

    static final int border = 10;
    static final int cribWidth = cardWidth * 2;
    static final int countSlots = 8;   // a count has at most 8 cards
    static final int countOffset = cardWidth * 3 / 5;
    static final int gap = 30;

    final CribbageDeckView cribView;

    FrenchCard starter;
    List<FrenchCard> count = List.of();
    int total;
    String phaseText = "";

    public CribbageTableView(CribbageGameState state, int humanId) {
        cribView = new CribbageDeckView(humanId, state.getCrib(), false,
                new Rectangle(border + cardWidth + gap, border + labelHeight, cribWidth, cardHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int top = border + labelHeight;
        g2.setColor(Color.white);
        g2.drawString("Starter", border, top - 4);
        g2.drawString("Crib", border + cardWidth + gap, top - 4);
        int countX = border + cardWidth + gap + cribWidth + gap;
        g2.drawString("Count: " + total, countX, top - 4);

        Rectangle starterRect = new Rectangle(border, top, cardWidth, cardHeight);
        if (starter != null)
            CribbageDeckView.drawCardAt(g2, starter, starterRect, true);
        else
            CribbageDeckView.drawCardAt(g2, null, starterRect, false);
        cribView.drawDeck(g2);
        for (int i = 0; i < count.size(); i++)
            CribbageDeckView.drawCardAt(g2, count.get(i),
                    new Rectangle(countX + i * countOffset, top, cardWidth, cardHeight), true);

        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD));
        g2.drawString(phaseText, border, top + cardHeight + 18);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = border + cardWidth + gap + cribWidth + gap + countOffset * (countSlots - 1) + cardWidth + border;
        return new Dimension(width, labelHeight + cardHeight + 2 * border + 18);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private static String gameOverText(CribbageGameState state) {
        int s0 = state.getScore(0), s1 = state.getScore(1);
        if (s0 == s1) return "Game over: a draw, " + s0 + " each";
        int winner = s0 > s1 ? 0 : 1;
        return "Game over: Player " + winner + " wins, " + Math.max(s0, s1) + " to " + Math.min(s0, s1);
    }

    public void update(CribbageGameState state) {
        starter = state.getStarter();
        cribView.updateComponent(state.getCrib());
        count = state.getPlaySequence();
        total = state.getRunningTotal();
        if (!state.isNotTerminal())
            phaseText = gameOverText(state);
        else if (state.getGamePhase() == CribbageGameState.CribbageGamePhase.Discard)
            phaseText = "Round " + (state.getRoundCounter() + 1) + ": Player " + state.getCurrentPlayer()
                    + " to discard two cards to the crib";
        else
            phaseText = "Round " + (state.getRoundCounter() + 1) + ": Player " + state.getCurrentPlayer() + " to play";
    }
}
