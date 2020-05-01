package games.pandemic;

import core.components.Card;
import core.content.Property;
import core.content.PropertyColor;
import core.content.PropertyString;
import org.davidmoten.text.utils.WordWrap;
import utilities.Hash;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;

public class PandemicCardView extends JComponent {
    private Image background;
    private Card card;
    private int width;
    private int height;

    public PandemicCardView(Card c, String backgroundPath) {
        this.card = c;
        if (backgroundPath != null && !backgroundPath.equals("")) {
            this.background = ImageIO.GetInstance().getImage(backgroundPath);
            width = background.getWidth(null);
            height = background.getHeight(null);
        } else {
            width = 100;
            height = 50;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCard((Graphics2D) g, width, height, card, background, 0, 0);
    }

    public static void drawCard(Graphics2D g, int width, int height, Card card, Image background, int x, int y) {
        // Draw card background
        if (background != null) {
            g.drawImage(background, x, y, null, null);
        } else {
            g.setColor(Color.lightGray);
            g.fillRect(x, y, width-1, height-1);
            g.setColor(Color.black);
        }

        if (card != null) {
            Property name = card.getProperty(Hash.GetInstance().hash("name"));
            Property color = card.getProperty(Hash.GetInstance().hash("color"));

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

            if (color != null) {
                g.setColor(Utils.stringToColor(((PropertyColor) color).valueStr));
            }
            g.drawRect(x, y, width - 1, height - 1);
        }
    }

    public void update(Card c) {
        this.card = c;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
