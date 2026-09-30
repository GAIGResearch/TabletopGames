package games.blackjack.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState;
import games.blackjack.BlackjackParameters;

import javax.swing.*;
import java.awt.*;

import static games.blackjack.gui.BlackjackGUIManager.*;

/**
 * The dealer's cards (the hole card face down until turned up), their total, and the hand number, stage and draw deck.
 */
public class BlackjackDealerView extends JComponent {

    static final int width = 480, border = 8;
    // the dealer has room to spread the cards further apart than a player
    static final int dealerStep = 2 * cardOffset;
    static final int handWidth = cardWidth + 6 * dealerStep;

    Deck<FrenchCard> dealerHand;
    FrenchCard hole;
    boolean showHole;
    String total = "", stage = "", handNumber = "", drawDeckText = "";

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillRoundRect(0, 0, width, getPreferredSize().height, 16, 16);
        g2.setColor(Color.white);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString("Dealer", border, border + 14);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
        g2.drawString(total, border, border + 22 + cardHeight + 18);
        if (dealerHand != null)
            drawHand(g2, dealerHand, border, border + 22, handWidth, dealerStep, hole, showHole);
        int textX = border + handWidth + 20;
        g2.drawString(handNumber, textX, border + 40);
        g2.drawString(stage, textX, border + 62);
        g2.drawString(drawDeckText, textX, border + 84);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, cardHeight + 2 * border + 50);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** Refresh from the state. {@code showHole} shows the hole card's face (full observability only). */
    public void update(BlackjackGameState state, boolean showHole) {
        BlackjackParameters params = (BlackjackParameters) state.getGameParameters();
        dealerHand = state.getDealerHand();
        hole = state.getHoleCard().getSize() == 0 ? null : state.getHoleCard().peek();
        this.showHole = showHole;
        if (dealerHand.getSize() == 0) {
            total = "";
        } else if (hole != null) {
            total = "showing " + describeTotal(dealerHand.getComponents());
        } else {
            total = state.dealerHasBlackjack() ? "Blackjack!" : describeTotal(dealerHand.getComponents());
        }
        handNumber = params.nHands > 1
                ? "Hand " + Math.min(state.getRoundCounter() + 1, params.nHands) + " of " + params.nHands : "";
        if (!state.isNotTerminal())
            stage = "Game over";
        else
            stage = switch ((BlackjackGameState.BlackjackGamePhase) state.getGamePhase()) {
                case Betting -> "Players are betting";
                case Insurance -> "Insurance offered";
                case Play -> "Players are playing";
            };
        drawDeckText = "Draw deck: " + state.getDrawDeck().getSize() + " cards";
    }
}
