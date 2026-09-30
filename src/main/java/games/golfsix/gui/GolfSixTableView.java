package games.golfsix.gui;

import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;

/**
 * The centre of the table: the draw deck, the top of the discard pile, and the card the current player has drawn.
 */
public class GolfSixTableView extends JComponent {

    static final int gap = 30, labelHeight = 20;

    int drawDeckSize, discardPileSize;
    FrenchCard topDiscard, drawnCard;
    boolean showDrawnCard;

    /**
     * Refresh from the state. drawnCard is null when no card has been drawn; with showDrawnCard false it is drawn
     * face-down.
     */
    public void update(int drawDeckSize, int discardPileSize, FrenchCard topDiscard, FrenchCard drawnCard,
                       boolean showDrawnCard) {
        this.drawDeckSize = drawDeckSize;
        this.discardPileSize = discardPileSize;
        this.topDiscard = topDiscard;
        this.drawnCard = drawnCard;
        this.showDrawnCard = showDrawnCard;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.white);
        Rectangle rect = cardRect(0);
        if (drawDeckSize > 0)
            g2.drawImage(CardArt.backOfCard(), rect.x, rect.y, rect.width, rect.height, null);
        g2.drawString("Draw deck (" + drawDeckSize + ")", rect.x, labelHeight - 6);

        rect = cardRect(1);
        if (topDiscard != null)
            CardArt.drawCardAt(g2, topDiscard, rect);
        g2.setColor(Color.white);
        g2.drawString("Discard pile (" + discardPileSize + ")", rect.x, labelHeight - 6);

        rect = cardRect(2);
        g2.drawString("Drawn card", rect.x, labelHeight - 6);
        if (drawnCard != null) {
            if (showDrawnCard)
                CardArt.drawCardAt(g2, drawnCard, rect);
            else
                g2.drawImage(CardArt.backOfCard(), rect.x, rect.y, rect.width, rect.height, null);
        } else {
            g2.setColor(new Color(255, 255, 255, 80));
            g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
        }
    }

    private Rectangle cardRect(int slot) {
        // the labels are wider than a card
        return new Rectangle(slot * (CardArt.cardWidth + gap + 20), labelHeight, CardArt.cardWidth, CardArt.cardHeight);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(3 * CardArt.cardWidth + 2 * (gap + 20), labelHeight + CardArt.cardHeight + 4);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }
}
