package gui;

import core.components.Area;
import core.components.Component;
import core.components.GridBoard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import static gui.GridBoardView.cellSize;

public class AreaView extends JComponent {
    Area area;
    int width, height;
    HashMap<Integer, Rectangle> drawMap;

    private Map.Entry<Integer, Rectangle> dragging;

    public AreaView(Area area, int width, int height) {
        updateArea(area);
        this.width = width;
        this.height = height;
        drawMap = new HashMap<>();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (Map.Entry<Integer, Rectangle> en: drawMap.entrySet()) {
                    if (en.getValue().contains(e.getPoint())) {
                        // clicked on this rectangle
                        dragging = en;
                        break;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging != null) {
                    dragging.getValue().setLocation(e.getPoint());
                    drawMap.put(dragging.getKey(), dragging.getValue());
                    repaint();
                    dragging = null;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging != null) {
                    dragging.getValue().setLocation(e.getPoint());
                    drawMap.put(dragging.getKey(), dragging.getValue());
                    repaint();
                    dragging = null;
                }
            }
        });
    }

    public void updateArea(Area area) {
        this.area = area;
        if (area != null) {
            setToolTipText("Component ID: " + area.getComponentID());
        }
    }

    public Area getArea() {
        return area;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawArea((Graphics2D) g);
    }

    protected void drawArea(Graphics2D g) {
        drawArea(g, area, 0, 0, width, height, drawMap);
    }


    public static void drawArea(Graphics2D g, Area area, int x, int y, int width, int height,
                                                      HashMap<Integer, Rectangle> drawMap) {

        // Draw components at (x, y)
        for (Map.Entry<Integer, Component> e: area.getComponents().entrySet()) {
            Component c = e.getValue();
            if (c instanceof GridBoard) {
                Rectangle r = drawMap.get(e.getKey());
                if (r == null) {
                    drawMap.put(e.getKey(), new Rectangle(0, 0, ((GridBoard) c).getWidth()*cellSize, ((GridBoard) c).getHeight()*cellSize));
                    GridBoardView.drawGridBoard(g, (GridBoard) c, 0, 0);
                } else {
                    GridBoardView.drawGridBoard(g, (GridBoard) c, r);
                }
            }
        }

        g.setColor(Color.black);
        g.drawRect(x, y, width, height);
    }
}
