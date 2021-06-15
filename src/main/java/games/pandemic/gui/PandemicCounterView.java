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
            if (color != null) {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            } else {
                g.setColor(Color.lightGray);
            }
            g.fillRect(0, 0, width-1, height-1);
            if (color != null) {
                g.setColor(color);
            }
            g.drawRect(0, 0, width-1, height-1);
        }

        g.setColor(Color.black);
        Font f = g.getFont();
        g.setFont(new Font(f.getName(), Font.BOLD, 32));
        int value = ((Counter)component).getValue();
        g.drawString(""+value, width/2-15, width-15);
        g.setFont(f);
    }
}
