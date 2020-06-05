package games.pandemic.gui;

import core.components.Counter;
import gui.views.CounterView;
import utilities.ImageIO;

import java.awt.*;

public class PandemicCounterView extends CounterView {
    private Image background;
    private Color color;

    public PandemicCounterView(Counter c, Color color, String backgroundPath) {
        super(c);
        this.color = color;
        if (backgroundPath != null && !backgroundPath.equals("")) {
            this.background = ImageIO.GetInstance().getImage(backgroundPath);
            width = Math.max(background.getWidth(null), background.getHeight(null));
            height = width;
        }
    }

    public void drawCounter(Graphics2D g) {
        // Draw background
        if (background != null) {
            g.drawImage(background, 0, 0, null, null);
        } else {
            g.setColor(Color.lightGray);
            g.fillOval(0, 0, width-1, height-1);
            g.setColor(Color.black);
        }

        int value = ((Counter)component).getValue();
        g.drawString(""+value, 5, 15);

        if (color != null) {
            g.setColor(color);
        }
        g.drawOval(0, 0, width-1, height-1);
    }

}
