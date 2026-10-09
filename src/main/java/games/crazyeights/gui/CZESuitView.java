package games.crazyeights.gui;

import core.components.FrenchCard;
import games.crazyeights.CZEGameState;
import games.crazyeights.CZEStarterSuitNomination;

import javax.swing.*;
import java.awt.*;

/**
 * The suit that must be matched. This is the one thing a player needs that is not a component on the table:
 * after an Eight is played the top discard's own suit is irrelevant, and only the nominated suit counts.
 * Also shows how many players have passed in succession, since that is what ends a blocked game.
 */
public class CZESuitView extends JComponent {

    // ♥ heart, ♦ diamond, ♣ club, ♠ spade
    private static final String[] SYMBOLS = {"♦", "♥", "♣", "♠"};  // order of FrenchCard.Suite

    static final int width = 180, height = 130;

    FrenchCard.Suite suit;
    boolean topIsEight;
    int passes, nPlayers;
    boolean awaitingNomination;

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.white);
        g2.fillRoundRect(0, 0, width - 1, height - 1, 12, 12);
        g2.setColor(Color.black);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 12, 12);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.drawString("Suit to match", 10, 20);

        if (suit != null) {
            boolean red = suit == FrenchCard.Suite.Hearts || suit == FrenchCard.Suite.Diamonds;
            g2.setColor(red ? new Color(190, 20, 20) : Color.black);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 48f));
            g2.drawString(SYMBOLS[suit.ordinal()], 12, 70);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
            g2.drawString(suit.name(), 62, 62);
        }

        g2.setColor(Color.darkGray);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        if (topIsEight)
            g2.drawString("(nominated - top card is an Eight)", 10, 88);
        if (awaitingNomination) {
            g2.drawString("Dealer is nominating the suit", 10, 104);
        } else {
            g2.drawString("Passes in a row: " + passes + " of " + nPlayers, 10, 104);
            if (passes + 1 >= nPlayers)
                g2.drawString("One more pass blocks the game", 10, 120);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    // BoxLayout consults all three, and would otherwise stretch or collapse this panel
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public void update(CZEGameState state) {
        suit = state.getCurrentSuit();
        topIsEight = state.getDiscardPile().getSize() > 0 && CZEGameState.isEight(state.getTopCard());
        passes = state.getConsecutivePasses();
        nPlayers = state.getNPlayers();
        // named explicitly rather than "any sequence", so a later sub-phase cannot inherit this caption
        awaitingNomination = state.currentActionInProgress() instanceof CZEStarterSuitNomination;
    }
}
