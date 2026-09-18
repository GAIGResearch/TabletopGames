package games.blackjack.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static games.blackjack.gui.BlackjackGUIManager.*;

/**
 * One player's area: their chips and insurance, and each of their hands (more than one after a split) with its
 * total and bet. The hand being played is outlined.
 */
public class BlackjackPlayerView extends JComponent {

    static final int border = 5, headerHeight = 20, handTextHeight = 32;
    static final int handSlotWidth = cardWidth + 4 * cardOffset;
    static final int handSlotHeight = cardHeight + handTextHeight;
    // two hands side by side; a second row only when splitting can make a third or fourth hand
    static final int columns = 2;

    final int playerId;
    final int rows;

    final List<Deck<FrenchCard>> hands = new ArrayList<>();
    final List<String> handTotals = new ArrayList<>();
    final List<String> handBets = new ArrayList<>();
    int activeHand = -1;
    String header = "";

    public BlackjackPlayerView(int playerId, boolean splitting) {
        this.playerId = playerId;
        this.rows = splitting ? 2 : 1;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Insets in = getInsets();
        int x0 = in.left + border, y0 = in.top + border;
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.drawString(header, x0, y0 + 13);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        for (int h = 0; h < hands.size() && h < columns * rows; h++) {
            int x = x0 + (h % columns) * (handSlotWidth + border);
            int y = y0 + headerHeight + (h / columns) * (handSlotHeight + border);
            drawHand(g2, hands.get(h), x, y, handSlotWidth, cardOffset, null, false);
            if (h == activeHand) {
                g2.setColor(Color.yellow);
                g2.setStroke(new BasicStroke(3));
                int cardsWidth = Math.max(cardWidth, Math.min(handSlotWidth,
                        cardWidth + (hands.get(h).getSize() - 1) * cardOffset));
                g2.drawRoundRect(x - 2, y - 2, cardsWidth + 4, cardHeight + 4, 8, 8);
                g2.setStroke(new BasicStroke(1));
                g2.setColor(Color.white);
            }
            g2.drawString(handTotals.get(h), x, y + cardHeight + 14);
            g2.drawString(handBets.get(h), x, y + cardHeight + 28);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Insets in = getInsets();
        return new Dimension(columns * handSlotWidth + (columns + 1) * border + in.left + in.right,
                headerHeight + rows * (handSlotHeight + border) + border + in.top + in.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** Refresh from the state. Player cards are dealt face up, so they are always shown. */
    public void update(BlackjackGameState state) {
        hands.clear();
        handTotals.clear();
        handBets.clear();
        List<Deck<FrenchCard>> playerHands = state.getPlayerHands(playerId);
        boolean inHand = false;
        for (int h = 0; h < playerHands.size(); h++) {
            Deck<FrenchCard> hand = playerHands.get(h);
            hands.add(hand);
            inHand |= hand.getSize() > 0;
            String total = hand.getSize() == 0 ? "" : describeTotal(hand.getComponents());
            if (hand.getSize() > 0 && state.isNatural(playerId, h))
                total = "Blackjack!";
            handTotals.add(total);
            int bet = state.getBet(playerId, h);
            handBets.add(bet > 0 ? "bet " + bet : "");
        }
        boolean playing = state.isNotTerminal() && state.getGamePhase() == BlackjackGameState.BlackjackGamePhase.Play
                && state.getCurrentPlayer() == playerId;
        activeHand = playing ? state.getActiveHand() : -1;

        boolean sittingOut = state.isNotTerminal() && !inHand && state.getBet(playerId, 0) == 0
                && state.getGamePhase() != BlackjackGameState.BlackjackGamePhase.Betting;
        header = "Chips " + state.getChips(playerId)
                + (state.getInsurance(playerId) > 0 ? "   insurance " + state.getInsurance(playerId) : "")
                + (sittingOut ? "   (sitting out)" : "");
    }
}
