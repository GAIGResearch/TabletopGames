package games.schwimmen.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.gui.CardArt;

import javax.swing.*;
import java.awt.*;

/**
 * The centre of the table: the extra hand (face down until the dealer chooses), the face-up table cards, the draw
 * deck and the discard pile.
 */
public class SchwimmenTableView extends JComponent {

    static final int gap = 8, groupGap = 40, labelHeight = 20;

    int extraHandSize, drawDeckSize, discardPileSize;
    Deck<FrenchCard> table;
    FrenchCard topDiscard;

    public void update(int extraHandSize, Deck<FrenchCard> table, int drawDeckSize, int discardPileSize,
                       FrenchCard topDiscard) {
        this.extraHandSize = extraHandSize;
        this.table = table;
        this.drawDeckSize = drawDeckSize;
        this.discardPileSize = discardPileSize;
        this.topDiscard = topDiscard;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(Color.white);

        // the table cards, or the extra hand face down while the dealer chooses
        int x = 0;
        if (extraHandSize > 0) {
            g2.drawString("Extra hand (the dealer's choice)", x, labelHeight - 6);
            for (int i = 0; i < extraHandSize; i++)
                g2.drawImage(CardArt.backOfCard(), x + i * (CardArt.cardWidth + gap), labelHeight,
                        CardArt.cardWidth, CardArt.cardHeight, null);
        } else if (table != null) {
            g2.drawString("Table", x, labelHeight - 6);
            for (int i = 0; i < table.getSize(); i++)
                CardArt.drawCardAt(g2, table.get(i),
                        new Rectangle(x + i * (CardArt.cardWidth + gap), labelHeight, CardArt.cardWidth, CardArt.cardHeight));
        }

        x = 3 * CardArt.cardWidth + 2 * gap + groupGap;
        g2.setColor(Color.white);
        g2.drawString("Draw deck (" + drawDeckSize + ")", x, labelHeight - 6);
        if (drawDeckSize > 0)
            g2.drawImage(CardArt.backOfCard(), x, labelHeight, CardArt.cardWidth, CardArt.cardHeight, null);

        x += CardArt.cardWidth + groupGap;
        g2.drawString("Discard pile (" + discardPileSize + ")", x, labelHeight - 6);
        if (topDiscard != null)
            CardArt.drawCardAt(g2, topDiscard, new Rectangle(x, labelHeight, CardArt.cardWidth, CardArt.cardHeight));
    }

    @Override
    public Dimension getPreferredSize() {
        // the labels are wider than a card, so the last one gets extra room
        return new Dimension(5 * CardArt.cardWidth + 2 * gap + 2 * groupGap + 40, labelHeight + CardArt.cardHeight + 4);
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
