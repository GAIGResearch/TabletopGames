package utilities;

import components.Counter;

import javax.swing.*;
import java.awt.*;

public class CounterView extends JComponent {
    private Image background;
    private Counter counter;
    private Color color;
    private int width;
    private int height;

    public CounterView(Counter c, Color color, String backgroundPath) {
        this.counter = c;
        this.color = color;
        if (backgroundPath != null && !backgroundPath.equals("")) {
            this.background = ImageIO.GetInstance().getImage(backgroundPath);
            width = background.getWidth(null);
            height = background.getHeight(null);
        } else {
            width = 20;
            height = 20;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCounter((Graphics2D) g);
    }

    public void drawCounter(Graphics2D g) {
        // Draw card background
        if (background != null) {
            g.drawImage(background, 0, 0, null, null);
        } else {
            g.setColor(Color.lightGray);
            g.fillOval(0, 0, width-1, height-1);
            g.setColor(Color.black);
        }

        int value = counter.getValue();
        g.drawString(""+value, 5, 15);

        if (color != null) {
            g.setColor(color);
        }
        g.drawOval(0, 0, width-1, height-1);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
