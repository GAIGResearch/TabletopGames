package gui.views;

import core.components.Card;
import gui.GUI;
import org.davidmoten.text.utils.WordWrap;

import java.awt.*;

public class CardView extends ComponentView {

    public CardView(Card c) {
        super(c, GUI.defaultCardWidth, GUI.defaultCardHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCard((Graphics2D) g);
    }

    public void drawCard(Graphics2D g) {
        drawCard(g, 0, 0, width, height, (Card) component, null, null, true);
    }

    public static void drawCard(Graphics2D g, Card card, Rectangle r) {
        drawCard(g, r.x, r.y, r.width, r.height, card, null, null, true);
    }

    public static void drawCard(Graphics2D g, Rectangle r, Card card, Image front, Image back, boolean visible) {
        drawCard(g, r.x, r.y, r.width, r.height, card, front, back, visible);
    }

    public static void drawCard(Graphics2D g, int x, int y, int width, int height, Card card, Image front, Image back, boolean visible) {
        if (visible && front != null) {
            g.drawImage(front, x, y, width, height, null, null);
        } else if (!visible && back != null) {
            g.drawImage(back, x, y, width, height, null, null);
        } else {
            // Draw background
            g.setColor(Color.lightGray);
            g.fillRect(x, y, width - 1, height - 1);
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
            g.drawRect(x, y, width - 1, height - 1);
        }
    }

}
