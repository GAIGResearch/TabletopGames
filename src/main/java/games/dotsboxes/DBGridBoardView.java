package games.dotsboxes;

import core.components.GridBoard;
import gui.views.ComponentView;

import java.awt.*;

import static core.AbstractGUI.defaultItemSize;

public class DBGridBoardView extends ComponentView {

    int dotSize = 6;
    Color[] colors = new Color[] {
            new Color(220, 97, 79),
            new Color(54, 200, 103),
            new Color(21, 187, 220),
            new Color(220, 215, 44),
            new Color(147, 71, 220)
    };
    Color[] edgeColors = new Color[] {
            new Color(155, 79, 68),
            new Color(45, 154, 77),
            new Color(16, 125, 150),
            new Color(146, 141, 34),
            new Color(103, 50, 155)
    };

    public DBGridBoardView(GridBoard<DBCell> gridBoard) {
        super(gridBoard, gridBoard.getWidth() * defaultItemSize, gridBoard.getHeight() * defaultItemSize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoard((Graphics2D)g, (GridBoard<DBCell>) component, dotSize/2, dotSize/2);
    }

    public void drawGridBoard(Graphics2D g, GridBoard<DBCell> gridBoard, int x, int y) {
        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                int xC = x + j * defaultItemSize;
                int yC = y + i * defaultItemSize;
                drawCell(g, gridBoard.getElement(j, i), xC, yC, x, y);
            }
        }
    }

    private void drawCell(Graphics2D g, DBCell element, int x, int y, int offsetX, int offsetY) {
        // Paint cell background, according to cell owner
        if (element.owner == -1) {
            g.setColor(new Color(228, 228, 228));
        } else {
            g.setColor(colors[element.owner]);
        }
        g.fillRect(x, y, defaultItemSize, defaultItemSize);

        // Draw 4 dots in the corners of the cell
        g.setColor(Color.black);
        g.fillOval(x - dotSize/2, y-dotSize/2, dotSize, dotSize);
        g.fillOval(x+defaultItemSize - dotSize/2, y-dotSize/2, dotSize, dotSize);
        g.fillOval(x-dotSize/2, y+defaultItemSize-dotSize/2, dotSize, dotSize);
        g.fillOval(x+defaultItemSize-dotSize/2, y+defaultItemSize-dotSize/2, dotSize, dotSize);

        // Draw cell owner
        g.drawString(element.toString(), x+defaultItemSize/2, y+defaultItemSize/2);

        // Draw cell edges
        Stroke s = g.getStroke();
        g.setStroke(new BasicStroke(3));
        for (DBEdge e: element.edges) {
            if (e.owner != -1) {
                // This edge exists, draw it!
                g.setColor(edgeColors[e.owner]);
                g.drawLine(e.from.getX() * defaultItemSize + offsetX, e.from.getY() * defaultItemSize + offsetY,
                        e.to.getX() * defaultItemSize + offsetX, e.to.getY() * defaultItemSize + offsetY);
            }
        }
        g.setStroke(s);
    }

}
