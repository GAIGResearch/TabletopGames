package gui.views;

import core.AbstractGUI;
import core.components.Card;
import core.components.Component;
import core.components.Deck;

import java.awt.*;

public class DeckView<T extends Component> extends ComponentView {
    protected boolean front;

    public DeckView(Deck<T> d, boolean visible) {
        super(d, AbstractGUI.defaultCardWidth, AbstractGUI.defaultCardHeight);
        this.front = visible;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawDeck((Graphics2D) g);
    }

    public void drawDeck(Graphics2D g) {
        drawDeck(g, (Deck<? extends Component>) component, component.getComponentName(), null,
                new Rectangle(0, 0, width, height), front);
    }

    public void setFront(boolean visible) {
        this.front = visible;
    }

    public void flip() {
        front = !front;
    }

    public static <T extends Component> void drawDeck(Graphics2D g, Deck<T> deck, String name, Image background,
                                                      Rectangle rect, boolean front) {
        if (background != null) {
            g.drawImage(background, rect.x, rect.y, null, null);
        } else {
            if (front && deck != null && deck.getSize() > 0) {
                Component c = deck.peek();
                if (c instanceof Card) {
                    // Draw cards, 0 index on top
                    for (int i = deck.getSize()-1; i >= 0; i--) {
                        Card card = (Card) deck.getComponents().get(i);
                        CardView.drawCard(g, rect.x, rect.y, rect.width, rect.height, card);
                    }
                }
            } else {
                g.setColor(Color.lightGray);
                g.fillRect(rect.x, rect.y, rect.width-1, rect.height-1);
                g.setColor(Color.black);
                g.drawLine(rect.x, rect.y, rect.x + rect.width - 1, rect.y + rect.height - 1);
            }
        }

        int size = g.getFont().getSize();
        if (name != null && !name.equals("")) {
            g.drawString(name, rect.x + 10, rect.y + size + 20);
        }
        if (deck != null) {
            g.drawString(""+deck.getSize(), rect.x + 10, rect.y + rect.height - size);
        }
        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
