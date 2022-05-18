package tools.descentTileBuild;

import core.components.GridBoard;
import games.descent2e.gui.DescentGridBoardView;
import gui.views.ComponentView;
import utilities.Vector2D;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static core.AbstractGUI.defaultItemSize;

public class TileBuildGridBoardView extends ComponentView {

    Vector2D highlight;

    public TileBuildGridBoardView(GridBoard<String> gridBoard) {
        super(gridBoard, (gridBoard.getWidth()+1) * defaultItemSize, (gridBoard.getHeight()+1) * defaultItemSize);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                GridBoard<String> gridBoard = (GridBoard<String>) component;
                for (int i = 0; i < gridBoard.getHeight(); i++) {
                    boolean found = false;
                    for (int j = 0; j < gridBoard.getWidth(); j++) {
                        Rectangle r = new Rectangle(j * defaultItemSize, i * defaultItemSize, defaultItemSize, defaultItemSize);
                        if (r.contains(e.getPoint())) {
                            highlight = new Vector2D(j, i);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        GridBoard<String> gridBoard = (GridBoard<String>) component;

        int width = gridBoard.getWidth() * defaultItemSize;
        int height = gridBoard.getHeight() * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(0, 0, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                drawCell((Graphics2D)g, gridBoard.getElement(j, i), j, i, gridBoard.getWidth(), gridBoard.getHeight(), 0, 0);
            }
        }

        if (highlight != null) {
            g.setColor(Color.cyan);
            Stroke s = ((Graphics2D) g).getStroke();
            ((Graphics2D) g).setStroke(new BasicStroke(3));
            g.drawRect(highlight.getX() * defaultItemSize - 1, highlight.getY() * defaultItemSize - 1,
                    defaultItemSize + 2, defaultItemSize + 2);
            ((Graphics2D) g).setStroke(s);
        }
    }

    private static void drawCell(Graphics2D g, String element, int x, int y, int gridWidth, int gridHeight,
                                 int offsetX, int offsetY) {
        int xC = offsetX + x * defaultItemSize;
        int yC = offsetY + y * defaultItemSize;

        // Paint cell background
        g.setColor(DescentGridBoardView.colorMap.get(element));
        g.fillRect(xC, yC, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
        g.drawRect(xC, yC, defaultItemSize, defaultItemSize);
    }

}
