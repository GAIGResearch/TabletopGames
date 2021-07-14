package gui.views;

import core.components.Dice;

import java.awt.*;

import static gui.GUI.defaultItemSize;

public class DieView extends ComponentView {

    public DieView(Dice d) {
        super(d, defaultItemSize, defaultItemSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawDie((Graphics2D) g);
    }

    public void drawDie(Graphics2D g) {
        drawDie(g, 0, 0, width, (Dice) component);
    }

    public static void drawDie(Graphics2D g, Dice die, Rectangle r) {
        drawDie(g, r.x, r.y, r.width, die);
    }

    public static void drawDie(Graphics2D g, int x, int y, int size, Dice die) {
        int fontSize = g.getFont().getSize();
        int nSides = 6;
        if (die != null) nSides = die.getNumberOfSides();

        // Draw background and border
        switch (nSides) {
            case 4:
                // d4
                g.setColor(Color.lightGray);
                g.fillPolygon(new int[]{x - size / 2, x, x + size / 2}, new int[]{y + size / 2, y - size / 2, y + size / 2}, 3);
                g.setColor(Color.black);
                g.drawPolygon(new int[]{x - size / 2, x, x + size / 2}, new int[]{y + size / 2, y - size / 2, y + size / 2}, 3);
                break;
            case 8:
            case 20:
                // d8 & d20 drawn as regular hexagons
                g.setColor(Color.lightGray);
                g.fillPolygon(new int[]{x - size / 2, x, x + size / 2, x + size / 2, x, x - size / 2},
                        new int[]{y - size / 4, y - size / 2, y - size / 4, y + size / 4, y + size / 2, y + size / 4}, 6);
                g.setColor(Color.black);
                g.drawPolygon(new int[]{x - size / 2, x, x + size / 2, x + size / 2, x, x - size / 2},
                        new int[]{y - size / 4, y - size / 2, y - size / 4, y + size / 4, y + size / 2, y + size / 4}, 6);
                break;
            case 10:
                // d10 drawn as squished hexagon
                g.setColor(Color.lightGray);
                g.fillPolygon(new int[]{x - size / 2, x, x + size / 2, x + size / 2, x, x - size / 2},
                        new int[]{y - size / 6, y - size / 2, y - size / 6, y + size / 6, y + size / 2, y + size / 6}, 6);
                g.setColor(Color.black);
                g.drawPolygon(new int[]{x - size / 2, x, x + size / 2, x + size / 2, x, x - size / 2},
                        new int[]{y - size / 6, y - size / 2, y - size / 6, y + size / 6, y + size / 2, y + size / 6}, 6);
                break;
            case 12:
                // d12 drawn as decagon
                g.setColor(Color.lightGray);
                g.fillPolygon(new int[]{x - size / 2, x - size / 4, x, x + size / 4, x + size / 2, x + size / 2, x + size / 4, x, x - size / 4, x - size / 2},
                        new int[]{y - size / 6, y - size / 3, y - size / 2, y - size / 3, y - size / 6, y + size / 6, y + size / 3, y + size / 2, y + size / 3, y + size / 6}, 10);
                g.setColor(Color.black);
                g.drawPolygon(new int[]{x - size / 2, x - size / 4, x, x + size / 4, x + size / 2, x + size / 2, x + size / 4, x, x - size / 4, x - size / 2},
                        new int[]{y - size / 6, y - size / 3, y - size / 2, y - size / 3, y - size / 6, y + size / 6, y + size / 3, y + size / 2, y + size / 3, y + size / 6}, 10);
                break;
            default:
                // Default rectangle
                g.setColor(Color.lightGray);
                g.fillRect(x, y, size - 1, size - 1);
                g.setColor(Color.black);
                g.drawRect(x, y, size - 1, size - 1);
                break;
        }

        // Draw die value
        int value = 0;
        if (die != null) {
            value = die.getValue();
        }
        g.drawString(""+value, x + fontSize/3, y + fontSize);

    }

}
