package games.descent.gui;

import core.components.GridBoard;
import gui.views.ComponentView;

import java.awt.*;
import java.util.HashMap;

import static core.AbstractGUI.defaultItemSize;

public class DescentGridBoardView extends ComponentView {

    public static HashMap<String, Color> colorMap = new HashMap<String, Color>() {{
        put("null", Color.gray);
        put(null, Color.gray);
        put("edge", Color.black);
        put("plain", Color.white);
        put("block", Color.red);
        put("lava", Color.orange);
        put("water", Color.blue);
        put("hazard", Color.green);
        put("open", Color.gray);
    }};

    public DescentGridBoardView(GridBoard<String> gridBoard) {
        super(gridBoard, gridBoard.getWidth() * defaultItemSize, gridBoard.getHeight() * defaultItemSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoard((Graphics2D)g, (GridBoard<String>) component, 0, 0);
    }

    public static void drawGridBoard(Graphics2D g, GridBoard<String> gridBoard, int x, int y) {
        int width = gridBoard.getWidth() * defaultItemSize;
        int height = gridBoard.getHeight() * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int xC = x + j * defaultItemSize;
                int yC = y + i * defaultItemSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC);
            }
        }
    }

    public static void drawGridBoard(Graphics2D g, GridBoard<String> gridBoard, Rectangle rect) {
        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(rect.x, rect.y, rect.width-1, rect.height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int x = rect.x + j * defaultItemSize;
                int y = rect.y + i * defaultItemSize;
                drawCell(g, gridBoard.getElement(j, i), x, y);
            }
        }
    }

    private static void drawCell(Graphics2D g, String element, int x, int y) {
        // Paint cell background
        g.setColor(colorMap.get(element));
        g.fillRect(x, y, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
        g.drawRect(x, y, defaultItemSize, defaultItemSize);
    }

}
