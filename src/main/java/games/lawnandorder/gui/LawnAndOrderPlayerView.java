package games.lawnandorder.gui;

import games.lawnandorder.LawnAndOrderGameState;
import games.lawnandorder.LawnAndOrderParameters;
import games.lawnandorder.components.LawnCard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static games.lawnandorder.gui.LawnCardArt.cardHeight;
import static games.lawnandorder.gui.LawnCardArt.cardWidth;

/**
 * One player's area: a status line, their hand, the card they have chosen this turn (face down), their lawn with
 * its matching groups, and their score tracks.
 */
public class LawnAndOrderPlayerView extends JComponent {

    static final int width = 720, height = 166;
    static final int cardsTop = 38;
    static final Rectangle handArea = new Rectangle(6, cardsTop, 200, cardHeight);
    static final Rectangle chosenArea = new Rectangle(216, cardsTop, cardWidth, cardHeight);
    static final Rectangle lawnArea = new Rectangle(290, cardsTop, width - 296, cardHeight);

    final int playerId;
    String header = "", groups = "", tracks = "";
    Color headerColour = Color.white;
    List<LawnCard> hand = new ArrayList<>(), lawn = new ArrayList<>();
    LawnCard chosen;
    boolean showCards;

    public LawnAndOrderPlayerView(int playerId) {
        this.playerId = playerId;
        // registers the view with the ToolTipManager, so getToolTipText(MouseEvent) is asked for each card
        setToolTipText("");
    }

    /**
     * The three attributes of the visible card under the mouse; the art alone is not always clear.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        LawnCard card = null;
        if (showCards) {
            card = cardAt(hand, handArea, e.getPoint());
            if (chosen != null && chosenArea.contains(e.getPoint()))
                card = chosen;
        }
        if (card == null)
            card = cardAt(played(), lawnArea, e.getPoint());
        if (card == null)
            return null;
        return "<html>" + LawnCard.Category.TYPE.subcommittee + ": " + card.type
                + "<br>" + LawnCard.Category.COLOUR.subcommittee + ": " + card.colour
                + "<br>" + LawnCard.Category.FEATURE.subcommittee + ": " + card.feature + "</html>";
    }

    /**
     * The card of the fan under the point; where cards overlap the later one is on top.
     */
    private static LawnCard cardAt(List<LawnCard> cards, Rectangle area, Point p) {
        for (int i = cards.size() - 1; i >= 0; i--)
            if (fanSlot(cards.size(), i, area).contains(p))
                return cards.get(i);
        return null;
    }

    // the lawn in the order it was played, the first card on the left
    private List<LawnCard> played() {
        List<LawnCard> played = new ArrayList<>(lawn);
        Collections.reverse(played);
        return played;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // a dark panel behind the cards, so the text is legible against the table
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
        g2.setColor(headerColour);
        g2.drawString(header, 8, 18);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
        g2.setColor(Color.white);
        g2.drawString("Hand", handArea.x + 2, cardsTop - 4);
        g2.drawString("Chosen", chosenArea.x + 2, cardsTop - 4);
        g2.drawString("Lawn (" + lawn.size() + ")", lawnArea.x + 2, cardsTop - 4);

        drawFan(g2, hand, handArea, showCards);
        if (chosen != null)
            LawnCardArt.drawCard(g2, chosen, chosenArea, showCards);
        else
            drawEmptySlot(g2, chosenArea);
        drawFan(g2, played(), lawnArea, true);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.setColor(Color.white);
        g2.drawString(groups, 8, cardsTop + cardHeight + 18);
        g2.drawString(tracks, 8, cardsTop + cardHeight + 36);
    }

    /**
     * The cards left to right, overlapping when there are too many for the area.
     */
    private static void drawFan(Graphics2D g, List<LawnCard> cards, Rectangle area, boolean faceUp) {
        if (cards.isEmpty()) {
            drawEmptySlot(g, new Rectangle(area.x, area.y, cardWidth, cardHeight));
            return;
        }
        for (int i = 0; i < cards.size(); i++)
            LawnCardArt.drawCard(g, cards.get(i), fanSlot(cards.size(), i, area), faceUp);
    }

    private static Rectangle fanSlot(int nCards, int i, Rectangle area) {
        int step = nCards == 1 ? 0 : Math.min(cardWidth + 4, (area.width - cardWidth) / (nCards - 1));
        return new Rectangle(area.x + i * step, area.y, cardWidth, cardHeight);
    }

    private static void drawEmptySlot(Graphics2D g, Rectangle r) {
        g.setColor(new Color(255, 255, 255, 90));
        g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    // GridBagLayout falls back to the minimum size when an area is too short, which would collapse the view
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /**
     * Refresh from the state.
     *
     * @param agentName the name shown after the player number
     * @param showCards false when this player's hand and chosen card are hidden from the viewer
     */
    public void update(LawnAndOrderGameState state, String agentName, boolean showCards) {
        this.showCards = showCards;
        hand = new ArrayList<>(state.getHand(playerId).getComponents());
        lawn = new ArrayList<>(state.getLawn(playerId).getComponents());
        chosen = state.getChosenCard(playerId).getSize() > 0 ? state.getChosenCard(playerId).get(0) : null;

        LawnAndOrderGameState.PlayerStatus status = state.getStatus(playerId);
        StringBuilder sb = new StringBuilder("Player " + playerId + " [" + agentName + "]   ");
        sb.append(switch (status) {
            case ACTIVE -> "Active";
            case PASSED -> "Passed";
            case CEASE_AND_DESIST -> "CEASE & DESIST";
        });
        if (status != LawnAndOrderGameState.PlayerStatus.CEASE_AND_DESIST)
            sb.append("   Citations ").append(state.getCitations(playerId));
        // only active players are checked against the limit
        if (status == LawnAndOrderGameState.PlayerStatus.ACTIVE)
            sb.append(" (limit ").append(state.getCitationLimit(playerId)).append(")");
        if (state.hasGoodwill(playerId))
            sb.append("   Goodwill");
        // whether a player has chosen is public; what they chose is not
        if (state.isNotTerminal() && status == LawnAndOrderGameState.PlayerStatus.ACTIVE
                && !state.getPlayersStillToChoose().contains(playerId))
            sb.append(state.getGamePhase() == LawnAndOrderGameState.Phase.PLAY_OBJECT ? "   (card chosen)" : "   (decided)");
        header = sb.toString();
        headerColour = switch (status) {
            case ACTIVE -> Color.white;
            case PASSED -> new Color(170, 230, 170);
            case CEASE_AND_DESIST -> new Color(255, 120, 120);
        };

        groups = "Matches: " + groupsText(lawn);
        StringBuilder t = new StringBuilder("Tracks: ");
        for (LawnCard.Category c : LawnCard.Category.values()) {
            int score = state.getTrackScore(playerId, c);
            t.append(c.subcommittee).append(' ').append(score)
                    .append(score >= ((LawnAndOrderParameters) state.getGameParameters()).targetScore ? " ★" : "")
                    .append("    ");
        }
        t.append("Total ").append((int) state.getGameScore(playerId));
        tracks = t.toString();
    }

    /**
     * Each attribute held by two or more lawn cards, with its count.
     */
    private static String groupsText(List<LawnCard> lawn) {
        StringBuilder sb = new StringBuilder();
        for (LawnCard.Attribute a : LawnCard.Attribute.values()) {
            long n = lawn.stream().filter(c -> c.has(a)).count();
            if (n >= 2)
                sb.append(sb.isEmpty() ? "" : ", ").append(a.label).append(" x").append(n);
        }
        return sb.isEmpty() ? "-" : sb.toString();
    }
}
