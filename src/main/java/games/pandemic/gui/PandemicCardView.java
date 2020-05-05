package games.pandemic.gui;

import core.components.Card;
import core.content.Property;
import core.content.PropertyColor;
import core.content.PropertyString;
import org.davidmoten.text.utils.WordWrap;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;

import static games.pandemic.PandemicConstants.colorHash;
import static games.pandemic.PandemicConstants.nameHash;

public class PandemicCardView extends JComponent {
    private Image background;
    private Card card;
    private int width;
    private int height;

    public static final int cardWidth = 100;
    public static final int cardHeight = 50;

    public PandemicCardView(Card c, String backgroundPath) {
        this.card = c;
        if (backgroundPath != null && !backgroundPath.equals("")) {
            this.background = ImageIO.GetInstance().getImage(backgroundPath);
            width = background.getWidth(null);
            height = background.getHeight(null);
        } else {
            width = cardWidth;
            height = cardHeight;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCard((Graphics2D) g, card, background, 0, 0, width, height);
    }

    public static void drawCard(Graphics2D g, Card card, Image background, Rectangle rect) {
        drawCard(g, card, background, rect.x, rect.y, rect.width, rect.height);
    }

    public static void drawDeckBack(Graphics2D g, String name, Image background, Rectangle rect) {
        if (background != null) {
            g.drawImage(background, rect.x, rect.y, null, null);
        } else {
            g.setColor(Color.lightGray);
            g.fillRect(rect.x, rect.y, rect.width-1, rect.height-1);
            g.setColor(Color.black);
        }

        int size = g.getFont().getSize();
        g.drawString(name, rect.x + 10, rect.y + size + 20);
        g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
    }

    public static void drawCard(Graphics2D g, Card card, Image background, int x, int y, int width, int height) {
        // Draw card background
        if (background != null) {
            g.drawImage(background, x, y, null, null);
        } else {
            g.setColor(Color.lightGray);
            g.fillRect(x, y, width-1, height-1);
            g.setColor(Color.black);
        }

        if (card != null) {
            Property name = card.getProperty(nameHash);
            PropertyColor col = (PropertyColor)card.getProperty(colorHash);
            if (col != null) {
                g.setColor(Utils.stringToColor(col.valueStr));
            }
            Stroke st = g.getStroke();
            g.setStroke(new BasicStroke(4));
            g.drawRect(x+1, y+1, width-2, height-2);
            g.setStroke(st);

            g.setColor(Color.black);

//        FontMetrics met = g.getFontMetrics();
            int w = (width * 2) / g.getFont().getSize();
            String wrapped =
                    WordWrap.from(((PropertyString) name).value)
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

//            g.drawRect(x, y, width - 1, height - 1);
        }
    }

    public void update(Card c) {
        this.card = c;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public Card getCard() {
        return card;
    }
}
