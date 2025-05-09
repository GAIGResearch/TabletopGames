package games.descent2e.descentTileBuild;

import core.components.BoardNode;
import core.components.GridBoard;
import games.descent2e.gui.DescentGridBoardView;
import gui.IScreenHighlight;
import gui.views.ComponentView;
import utilities.Vector2D;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static gui.AbstractGUIManager.defaultItemSize;

public class TileBuildGridBoardView extends ComponentView implements IScreenHighlight {

    Vector2D highlight;
    Vector2D oldHighlight;
    TileBuildState gs;
    ArrayList<Vector2D> path;

    final static Stroke dashedStroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
            0, new float[]{9}, 0);
    final static Stroke boldStroke = new BasicStroke(3);

    public TileBuildGridBoardView(TileBuildState gs, GridBoard gridBoard) {
        super(gridBoard, (gridBoard.getWidth()+1) * defaultItemSize, (gridBoard.getHeight()+1) * defaultItemSize);
        this.gs = gs;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    GridBoard gridBoard = (GridBoard) component;
                    for (int i = 0; i < gridBoard.getHeight(); i++) {
                        boolean found = false;
                        for (int j = 0; j < gridBoard.getWidth(); j++) {
                            Rectangle r = new Rectangle(j * defaultItemSize, i * defaultItemSize, defaultItemSize, defaultItemSize);
                            if (r.contains(e.getPoint())) {
                                if (highlight != null) {
                                    oldHighlight = highlight.copy();
                                }
                                highlight = new Vector2D(j, i);
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                } else {
                    oldHighlight = null;
                    highlight = null;
                }
                path = null;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Stroke s = ((Graphics2D) g).getStroke();
        GridBoard gridBoard = (GridBoard) component;

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
            ((Graphics2D) g).setStroke(boldStroke);
            g.drawRect(highlight.getX() * defaultItemSize - 1, highlight.getY() * defaultItemSize - 1,
                    defaultItemSize + 2, defaultItemSize + 2);
            ((Graphics2D) g).setStroke(s);
        }
        if (oldHighlight != null) {
            g.setColor(Color.gray);
            ((Graphics2D) g).setStroke(dashedStroke);
            g.drawRect(oldHighlight.getX() * defaultItemSize - 1, oldHighlight.getY() * defaultItemSize - 1,
                    defaultItemSize + 2, defaultItemSize + 2);
            ((Graphics2D) g).setStroke(s);
        }

        if (path != null) {
            for (int i = 0; i < path.size(); i++) {
                Vector2D point = path.get(i);
                g.setColor(Color.yellow);
                g.fillRect(point.getX() * defaultItemSize + defaultItemSize / 4, point.getY() * defaultItemSize + defaultItemSize / 4,
                        defaultItemSize / 2, defaultItemSize / 2);
                if (i < path.size() - 1) {
                    Vector2D nextPoint = path.get(i + 1);
                    ((Graphics2D) g).setStroke(boldStroke);
                    g.drawLine(point.getX() * defaultItemSize + defaultItemSize / 2, point.getY() * defaultItemSize + defaultItemSize / 2,
                            nextPoint.getX() * defaultItemSize + defaultItemSize / 2, nextPoint.getY() * defaultItemSize + defaultItemSize / 2);
                    ((Graphics2D) g).setStroke(s);
                }
            }
        }
    }

    private static void drawCell(Graphics2D g, BoardNode element, int x, int y, int gridWidth, int gridHeight,
                                 int offsetX, int offsetY) {
        if (element == null) return;

        int xC = offsetX + x * defaultItemSize;
        int yC = offsetY + y * defaultItemSize;

        // Paint cell background
        g.setColor(DescentGridBoardView.colorMap.get(element.getComponentName()));
        g.fillRect(xC, yC, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
        g.drawRect(xC, yC, defaultItemSize, defaultItemSize);
    }

    @Override
    public void clearHighlights() {
        highlight = null;
        oldHighlight = null;
    }
}
