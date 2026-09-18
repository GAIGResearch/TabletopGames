package games.agram.gui;

import core.components.FrenchCard;
import games.agram.AgramGameState;
import games.agram.AgramParameters;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static games.agram.gui.AgramGUIManager.*;

/**
 * The centre of the table: the trick in progress, in the order played, with who played each card and which card is
 * currently winning; plus the deal and trick numbers and the suit led.
 */
public class AgramTrickView extends JComponent {

    static final int gap = 10, margin = 12;
    static final int headerHeight = 44, footerHeight = 34;

    final int maxCards;
    final int width, height;

    List<FrenchCard> cards = new ArrayList<>();
    List<Integer> players = new ArrayList<>();
    int winningIndex = -1;
    String header = "", leadText = "", footer = "";
    boolean gameOver;

    public AgramTrickView(int nPlayers) {
        this.maxCards = nPlayers;
        this.width = Math.max(360, margin * 2 + nPlayers * (cardWidth + gap));
        this.height = headerHeight + cardHeight + 22 + footerHeight;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(255, 255, 255, 215));
        g2.fillRoundRect(0, 0, width - 1, height - 1, 14, 14);
        g2.setColor(Color.black);
        g2.drawRoundRect(0, 0, width - 1, height - 1, 14, 14);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString(header, margin, 20);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString(leadText, margin, 37);

        int y = headerHeight;
        for (int i = 0; i < cards.size(); i++) {
            int x = margin + i * (cardWidth + gap);
            AgramDeckView.drawCardAt(g2, cards.get(i), new Rectangle(x, y, cardWidth, cardHeight));
            if (i == winningIndex) {
                g2.setColor(new Color(220, 150, 20));
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(x - 2, y - 2, cardWidth + 4, cardHeight + 4, 8, 8);
                g2.setStroke(new BasicStroke(1));
            }
            g2.setColor(Color.black);
            g2.setFont(g2.getFont().deriveFont(i == winningIndex ? Font.BOLD : Font.PLAIN, 12f));
            String label = "Player " + players.get(i) + (i == 0 ? " (led)" : "");
            g2.drawString(label, x, y + cardHeight + 16);
        }
        if (cards.isEmpty() && !gameOver) {
            g2.setColor(Color.darkGray);
            g2.drawString("No cards played to this trick yet", margin, y + cardHeight / 2);
        }

        g2.setColor(Color.darkGray);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.drawString(footer, margin, height - 12);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public void update(AgramGameState state) {
        AgramParameters params = (AgramParameters) state.getGameParameters();
        int n = state.getNPlayers();
        gameOver = !state.isNotTerminal();
        cards = new ArrayList<>(state.getCurrentTrick().getComponents());
        players = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++)
            players.add(state.playerOfTrickCard(i));
        winningIndex = cards.isEmpty() ? -1 : players.indexOf(state.currentTrickWinner());

        int tricksDone = state.getDiscardPile().getSize() / n;
        int trick = Math.min(tricksDone + 1, params.nCardsPerPlayer);
        header = (params.nDeals > 1 ? "Deal " + (state.getRoundCounter() + 1) + " of " + params.nDeals + "   " : "")
                + "Trick " + trick + " of " + params.nCardsPerPlayer
                + (trick == params.nCardsPerPlayer ? "  - the last trick wins" : "");
        FrenchCard.Suite lead = state.getLeadSuit();
        if (!state.isNotTerminal()) {
            // trickLeader is the winner of the last trick, which has gone to the discard pile
            header = params.nDeals > 1 ? "Match over" : "Game over";
            leadText = "Player " + state.getTrickLeader() + " won the last trick"
                    + (params.nDeals > 1 ? " of the last deal" : " and the game");
        } else leadText = lead == null
                ? "Player " + state.getTrickLeader() + " to lead"
                : "Suit led: " + SUIT_SYMBOLS[lead.ordinal()] + " " + lead.name()
                  + "   -   highest " + lead.name() + " wins";
        footer = "Tricks played this deal: " + tricksDone + "     Cards not dealt: "
                + state.getDrawDeck().getSize() + " (unused)";
    }
}
