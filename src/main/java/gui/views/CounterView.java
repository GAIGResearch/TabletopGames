package gui.views;

import core.components.Counter;

import java.awt.*;

import static gui.GUI.defaultItemSize;

public class CounterView extends ComponentView {

    public CounterView(Counter c) {
        super(c, defaultItemSize, defaultItemSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawCounter((Graphics2D) g);
    }

    public void drawCounter(Graphics2D g) {
        drawCounter(g, 0, 0, width, (Counter) component);
    }


    public static void drawCounter(Graphics2D g, Counter counter, Rectangle r) {
        drawCounter(g, r.x, r.y, r.width, counter);
    }

    public static void drawCounter(Graphics2D g, int x, int y, int size, Counter counter) {
        int fontSize = g.getFont().getSize();
        int counterSize = size - fontSize;
        int counterX = x + size/2 - counterSize/2;
        int counterY = y + size/2 - counterSize/2;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillOval(counterX, counterY, counterSize-1, counterSize-1);
        g.setColor(Color.black);

        // Draw counter value
        int value = 0;
        if (counter != null) {
            value = counter.getValue();
        }
        g.drawString(""+value, counterX + fontSize/3, counterY + fontSize);

        // Draw border
        g.drawOval(counterX, counterY, counterSize-1, counterSize-1);

        // Draw counter name underneath
        String name = "Counter";
        if (counter != null) {
            name = counter.getComponentName();
        }
        g.drawString(name, x, y + counterSize + fontSize);
    }

}
