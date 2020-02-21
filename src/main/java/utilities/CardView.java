package utilities;

import components.Card;
import content.Property;
import content.PropertyColor;
import content.PropertyString;
import org.davidmoten.text.utils.WordWrap;

import javax.swing.*;
import java.awt.*;

public class CardView extends JComponent {
    private Image background;
    private Card card;
    private int width;
    private int height;

    public CardView(Card c, String backgroundPath) {
        this.card = c;
        if (backgroundPath != null && !backgroundPath.equals("")) {
            this.background = ImageIO.GetInstance().getImage(backgroundPath);
            width = background.getWidth(null);
            height = background.getHeight(null);
        } else {
            width = 100;
            height = 150;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCard((Graphics2D) g);
    }

    private void drawCard(Graphics2D g) {
        // Draw card background
        if (background != null) {
            g.drawImage(background, 0, 0, null, null);
        }

        Property name = card.getProperty(Hash.GetInstance().hash("name"));
        Property color = card.getProperty(Hash.GetInstance().hash("color"));

        String wrapped =
                WordWrap.from(((PropertyString)name).value)
                        .maxWidth((width-20)/getFont().getSize())
                        .insertHyphens(true) // true is the default
                        .wrap();
        String[] wraps = wrapped.split("-");
        int size = getFont().getSize();
        int i = 0;
        for (String s: wraps) {
            g.drawString(s, 10, i*size + 20);
            i++;
        }

        if (color != null) {
            g.setColor(Utils.stringToColor(((PropertyColor)color).valueStr));
        }
        g.drawRect(0,0, width-1, height-1);
    }

    public void update(Card c) {
        this.card = c;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
