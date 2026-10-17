package games.skitgubbe.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.skitgubbe.SkitgubbeGameState;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The middle of the table: the draw deck and trump card, the current trick (oldest card on the left), the bounced
 * cards waiting for the next trick's winner, and a summary of the state.
 */
public class SkitgubbeTableView extends JComponent {

    private final Dimension size;
    private final Image back = SkitgubbeDeckView.backImage();

    private boolean phaseOne;
    private int drawDeckSize, discardSize, trickSize;
    // the trump card, or null when there is none to show face up (not yet drawn, or hidden from the viewer)
    private FrenchCard trumpCard;
    private boolean trumpCardFaceDown;
    private String trumpSuit;
    // the trick from its bottom card up
    private final List<FrenchCard> trick = new ArrayList<>();
    private final List<String> trickLabels = new ArrayList<>();
    private final List<List<FrenchCard>> held = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();

    public SkitgubbeTableView(int width, int height) {
        size = new Dimension(width, height);
        setOpaque(false);
    }

    /**
     * @param trumpVisible whether the viewer may see the trump card while it is face down in phase one
     */
    public void update(SkitgubbeGameState state, boolean trumpVisible) {
        int nPlayers = state.getNPlayers();
        int current = state.getCurrentPlayer();
        phaseOne = state.getGamePhase() == SkitgubbeGameState.Phase.PHASE_ONE;
        drawDeckSize = state.getDrawDeck().getSize();
        discardSize = state.getDiscardPile().getSize();
        trickSize = state.getTrickSize();
        Deck<FrenchCard> trump = state.getTrumpCard();
        trumpCardFaceDown = trump.getSize() > 0 && !trumpVisible;
        trumpCard = trump.getSize() > 0 && trumpVisible ? trump.peek() : null;
        trumpSuit = state.getTrumpSuit() == null ? null : state.getTrumpSuit().name();

        trick.clear();
        trickLabels.clear();
        List<FrenchCard> cards = state.getTrick().getComponents();
        for (int i = cards.size() - 1; i >= 0; i--)
            trick.add(cards.get(i));
        if (phaseOne && trick.size() == 1)
            // the leader has played and the follower (the current player) is to answer
            trickLabels.add("P" + (current + nPlayers - 1) % nPlayers);

        held.clear();
        for (int p = 0; p < nPlayers; p++)
            held.add(new ArrayList<>(state.getHeldCards(p).getComponents()));

        lines.clear();
        if (!state.isNotTerminal()) {
            lines.add("Game over. " + result(state));
        } else if (phaseOne) {
            lines.add("Phase one: two-card tricks, suits do not matter");
            lines.add(trick.isEmpty()
                    ? "Player " + current + " leads to Player " + (current + 1) % nPlayers
                    : "Player " + current + " answers Player " + (current + nPlayers - 1) % nPlayers);
            if (state.getTrumpPlayer() >= 0)
                lines.add("Player " + state.getTrumpPlayer() + " drew the trump card and leads phase two");
        } else {
            lines.add("Phase two: beat the top card or pick it up. Trumps: " + trumpSuit);
            lines.add(trick.isEmpty()
                    ? "Player " + current + " leads"
                    : "Player " + current + " beats the top card or picks it up");
            lines.add("Cards in the trick: " + trick.size() + " of " + trickSize + "   Discarded: " + discardSize);
        }
        StringBuilder out = new StringBuilder();
        for (int p = 0; p < nPlayers; p++)
            if (state.getExitScore(p) > 0)
                out.append(out.length() == 0 ? "Out: " : ", ").append("P").append(p).append(" (").append(state.getExitScore(p)).append(")");
        if (out.length() > 0) lines.add(out.toString());
        repaint();
    }

    private static String result(SkitgubbeGameState state) {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("P").append(p).append(" ").append(ordinal(state.getOrdinalPosition(p)));
        }
        return sb.toString();
    }

    private static String ordinal(int n) {
        return n + (n == 1 ? "st" : n == 2 ? "nd" : n == 3 ? "rd" : "th");
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        int cw = SkitgubbeGUIManager.CARD_WIDTH, ch = SkitgubbeGUIManager.CARD_HEIGHT;
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));

        // top row: draw deck, trump card, discard pile
        int y = 22;
        g.drawString("Draw deck", 10, y);
        g.drawString("Trump card", 130, y);
        if (!phaseOne) g.drawString("Discarded", 250, y);
        y += 6;
        if (drawDeckSize > 0) g.drawImage(back, 10, y, cw, ch, null);
        g.drawString(String.valueOf(drawDeckSize), 10 + cw + 6, y + ch / 2);
        if (trumpCard != null) g.drawImage(SkitgubbeDeckView.cardImage(trumpCard), 130, y, cw, ch, null);
        else if (trumpCardFaceDown) g.drawImage(back, 130, y, cw, ch, null);
        else if (trumpSuit != null) g.drawString(trumpSuit, 130, y + ch / 2);
        if (!phaseOne) {
            if (discardSize > 0) g.drawImage(back, 250, y, cw, ch, null);
            g.drawString(String.valueOf(discardSize), 250 + cw + 6, y + ch / 2);
        }

        // the trick, from its bottom card; in phase two only the top card is to beat, so the others are dimmed
        y += ch + 26;
        g.drawString("Trick", 10, y);
        y += 6;
        int step = trick.size() <= 1 ? 0 : Math.min(cw + 8, (size.width - 20 - cw) / (trick.size() - 1));
        for (int i = 0; i < trick.size(); i++) {
            boolean top = i == trick.size() - 1;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, phaseOne || top ? 1f : 0.55f));
            g.drawImage(SkitgubbeDeckView.cardImage(trick.get(i)), 10 + i * step, y, cw, ch, null);
        }
        g.setComposite(AlphaComposite.SrcOver);
        for (int i = 0; i < trickLabels.size(); i++)
            g.drawString(trickLabels.get(i), 10 + i * step, y + ch + 15);

        // bounced cards, beside their owner's number
        y += ch + 44;
        if (phaseOne) {
            g.drawString("Bounced cards (go to the next trick's winner)", 10, y);
            y += 6;
            int x = 10;
            for (int p = 0; p < held.size(); p++) {
                if (held.get(p).isEmpty()) continue;
                g.drawString("P" + p, x, y + ch / 2);
                x += 26;
                for (FrenchCard card : held.get(p)) {
                    g.drawImage(SkitgubbeDeckView.cardImage(card), x, y, cw, ch, null);
                    x += 16;
                }
                x += cw + 10;
            }
            y += ch + 24;
        }

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
