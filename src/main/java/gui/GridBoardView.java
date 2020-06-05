package gui;

import core.components.GridBoard;

import javax.swing.*;
import java.awt.*;

public class GridBoardView<T> extends JComponent {
    GridBoard<T> gridBoard;
    int width, height;

    public static int cellSize = 20;

    public GridBoardView(GridBoard<T> gridBoard) {
        updateBoard(gridBoard);
        width = gridBoard.getWidth() * cellSize;
        height = gridBoard.getHeight() * cellSize;
    }

    public void updateBoard(GridBoard<T> gridBoard) {
        this.gridBoard = gridBoard;
        if (gridBoard != null) {
            setToolTipText("Component ID: " + gridBoard.getComponentID());
        }
    }

    public GridBoard<T> getGridBoard() {
        return gridBoard;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoard((Graphics2D)g, gridBoard, 0, 0);
    }

    public static <T> void drawGridBoard(Graphics2D g, GridBoard<T> gridBoard, int x, int y) {
        int width = gridBoard.getWidth() * cellSize;
        int height = gridBoard.getHeight() * cellSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int xC = x + j * cellSize;
                int yC = y + i * cellSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC);
            }
        }
    }

    public static <T> void drawGridBoard(Graphics2D g, GridBoard<T> gridBoard, Rectangle rect) {
        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(rect.x, rect.y, rect.width-1, rect.height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int x = rect.x + j * cellSize;
                int y = rect.y + i * cellSize;
                drawCell(g, gridBoard.getElement(j, i), x, y);
            }
        }
    }

    private static <T> void drawCell(Graphics2D g, T element, int x, int y) {
        // Paint cell background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, cellSize, cellSize);
        g.setColor(Color.black);
        g.drawRect(x, y, cellSize, cellSize);

        // Paint element in cell
        g.drawString(element.toString(), x + cellSize/2, y + cellSize);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}
