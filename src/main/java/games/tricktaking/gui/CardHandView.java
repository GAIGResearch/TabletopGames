package games.tricktaking.gui;

import core.components.Component;
import core.components.Deck;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * One player's area: their hand, and a status line with the number of cards, whatever the game adds (tricks won,
 * deals won) and the suits they are known to be void in. The cards are drawn with the given {@link CardFace}.
 *
 * @param <C> the type of card
 * @param <S> the type of suit
 */
public class CardHandView<C extends Component, S> extends JComponent {

    final CardFace<C, S> face;
    final int playerAreaWidth;
    final CardDeckView<C> handView;
    final int border = 5, borderBottom = 38;   // room for the status line and the titled border below it

    int nCards;
    String status = "";
    // for a clickable GUI: the cards to outline (deck index to colour), and the tooltip for a mouse position
    Map<Integer, Color> outlines = new HashMap<>();
    Function<MouseEvent, String> toolTips;

    public CardHandView(CardFace<C, S> face, Deck<C> hand, int playerId, int playerAreaWidth) {
        this.face = face;
        this.playerAreaWidth = playerAreaWidth;
        this.handView = new CardDeckView<>(face, playerId, hand, false,
                new Rectangle(border, border, playerAreaWidth, face.cardHeight()));
        this.handView.setDisplayOrder(face.handOrder());
        this.handView.setRightmostOnTop(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // a dark panel behind the cards, so the status line is legible against the table
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - borderBottom + 22, 12, 12);
        handView.drawDeck(g2);
        g2.setStroke(new BasicStroke(3));
        for (Map.Entry<Integer, Color> outline : outlines.entrySet()) {
            Shape shape = handView.visibleCardShape(outline.getKey());
            if (shape != null) {
                g2.setColor(outline.getValue());
                g2.draw(shape);
            }
        }
        g2.setStroke(new BasicStroke(1));
        g2.setColor(Color.white);
        g2.drawString(status, border, border + face.cardHeight() + 16);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(playerAreaWidth + border * 2, face.cardHeight() + border + borderBottom);
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
     * @param showHand false when this player's cards are hidden from the viewer
     * @param voids    the suits the player is known to be void in
     * @param extra    what the game adds to the status line (empty for nothing)
     */
    public void update(Deck<C> hand, boolean showHand, Set<S> voids, String extra) {
        handView.updateComponent(hand);
        handView.setFront(showHand);
        nCards = hand.getSize();
        StringBuilder voidText = new StringBuilder();
        for (S suit : face.suits())
            if (voids.contains(suit))
                voidText.append(voidText.isEmpty() ? "" : " ").append(face.suitSymbol(suit));
        status = nCards + (nCards == 1 ? " card" : " cards")
                + (extra.isEmpty() ? "" : ",  " + extra)
                + (voidText.isEmpty() ? "" : ",  void in " + voidText);
    }

    public CardDeckView<C> getHandView() {
        return handView;
    }

    /**
     * The deck index of the card shown at point p (in this view's coordinates), or -1 for none.
     */
    public int cardIndexAt(Point p) {
        return handView.cardIndexAt(p);
    }

    /**
     * Outlines the showing part of each card with the deck index given, in its colour; the rest are not outlined.
     */
    public void setOutlines(Map<Integer, Color> outlines) {
        this.outlines = new HashMap<>(outlines);
        repaint();
    }

    /**
     * Shows a tooltip computed from the mouse position (null for none).
     */
    public void setToolTips(Function<MouseEvent, String> toolTips) {
        this.toolTips = toolTips;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        return toolTips == null ? null : toolTips.apply(e);
    }
}
