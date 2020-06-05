package gui;

import core.AbstractGUI;
import core.components.Card;
import org.davidmoten.text.utils.WordWrap;

import javax.swing.*;
import java.awt.*;

public class CardView extends JComponent {
    protected Card card;
    protected int width, height;

    public CardView(Card c) {
        updateCard(c);
        width = AbstractGUI.defaultCardWidth;
        height = AbstractGUI.defaultCardHeight;
    }

    public void updateCard(Card c) {
        this.card = c;
        if (c != null) {
            setToolTipText("Component ID: " + c.getComponentID());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCard((Graphics2D) g);
    }

    public void drawCard(Graphics2D g) {
        drawCard(g, 0, 0, width, height, card);
    }

    public static void drawCard(Graphics2D g, int x, int y, int width, int height, Card card) {
        // Draw background
        g.setColor(Color.lightGray);
        g.fillOval(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw card name and owner
        String value = "Card";
        if (card != null) {
            value = card.getComponentName() + "(" + card.getOwnerId() + ")";
        }
        int w = (width * 2 - 10) / g.getFont().getSize();
        String wrapped =
                WordWrap.from(value)
                        .maxWidth(w)
                        .insertHyphens(true) // true is the default
                        .wrap();
        String[] wraps = wrapped.split("\n");
        int size = g.getFont().getSize();

        int i = 0;
        for (String s : wraps) {
            g.drawString(s, x + 10, y + i * size + 20);
            i++;
        }

        // Draw border
        g.drawRect(x, y, width-1, height-1);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public Card getCard() {
        return card;
    }
}
