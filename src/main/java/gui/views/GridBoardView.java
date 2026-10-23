package gui.views;

import core.components.Component;
import core.components.GridBoard;

import java.awt.*;

import static gui.GUI.defaultItemSize;

public class GridBoardView extends ComponentView {

    public GridBoardView(GridBoard gridBoard) {
        super(gridBoard, gridBoard.getWidth() * defaultItemSize, gridBoard.getHeight() * defaultItemSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoard((Graphics2D)g, (GridBoard) component, 0, 0);
    }

    public static void drawGridBoard(Graphics2D g, GridBoard gridBoard, int x, int y) {
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

    public static void drawGridBoard(Graphics2D g, GridBoard gridBoard, Rectangle rect) {
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

    private static <T extends Component> void drawCell(Graphics2D g, T element, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
        g.drawRect(x, y, defaultItemSize, defaultItemSize);

        // Paint element in cell
        g.drawString(element.toString(), x + defaultItemSize /2, y + defaultItemSize);
    }

}
