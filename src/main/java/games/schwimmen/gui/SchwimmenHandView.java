package games.schwimmen.gui;

import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;

/**
 * One player's hand, with a status line underneath. A card every player has seen taken from the table is marked
 * with a dot.
 */
public class SchwimmenHandView extends JComponent {

    static final int gap = 8, border = 6, statusHeight = 22, handSize = 3;

    PartialObservableDeck<FrenchCard> hand;
    boolean[] faceUp = new boolean[0];
    boolean[] seenByAll = new boolean[0];
    String status = "";

    /**
     * Refresh from the state. faceUp[i] says whether card i is drawn face up for this viewer, and seenByAll[i]
     * whether every player saw it taken from the table.
     */
    public void update(PartialObservableDeck<FrenchCard> hand, boolean[] faceUp, boolean[] seenByAll, String status) {
        this.hand = hand;
        this.faceUp = faceUp;
        this.seenByAll = seenByAll;
        this.status = status;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // the content sits inside the (titled) border
        Insets insets = getInsets();
        g2.translate(insets.left, insets.top);
        Dimension content = contentSize();
        // a dark panel behind the cards, so the status line is legible against the table
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(0, 0, content.width, content.height, 12, 12);
        if (hand != null) {
            for (int i = 0; i < hand.getSize(); i++) {
                Rectangle rect = cardRect(i);
                if (faceUp[i])
                    CardArt.drawCardAt(g2, hand.get(i), rect);
                else
                    g2.drawImage(CardArt.backOfCard(), rect.x, rect.y, rect.width, rect.height, null);
                if (seenByAll[i]) {
                    g2.setColor(new Color(255, 200, 0));
                    g2.fillOval(rect.x + rect.width - 16, rect.y + rect.height - 16, 12, 12);
                }
            }
        }
        g2.setColor(Color.white);
        g2.drawString(status, border, content.height - 7);
        g2.translate(-insets.left, -insets.top);
    }

    private Rectangle cardRect(int i) {
        return new Rectangle(border + i * (CardArt.cardWidth + gap), border, CardArt.cardWidth, CardArt.cardHeight);
    }

    private Dimension contentSize() {
        return new Dimension(2 * border + handSize * CardArt.cardWidth + (handSize - 1) * gap,
                2 * border + CardArt.cardHeight + statusHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension content = contentSize();
        Insets insets = getInsets();
        return new Dimension(content.width + insets.left + insets.right, content.height + insets.top + insets.bottom);
    }

    // GridBagLayout and FlowLayout fall back to the minimum size when an area is too small
    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
