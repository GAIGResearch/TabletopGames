package games.descent.gui;

import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyVector2D;
import gui.views.ComponentView;
import utilities.Vector2D;

import java.awt.*;
import java.util.HashMap;

import static core.AbstractGUI.defaultItemSize;
import static core.CoreConstants.coordinateHash;

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

    private GraphBoard masterGraph;

    public DescentGridBoardView(GridBoard<String> gridBoard, GraphBoard masterGraph) {
        super(gridBoard, gridBoard.getWidth() * defaultItemSize, gridBoard.getHeight() * defaultItemSize);
        this.masterGraph = masterGraph;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoardWithGraphConnectivity((Graphics2D)g, (GridBoard<String>) component, 0, 0, masterGraph);
    }

    public static void drawGridBoardWithGraphConnectivity(Graphics2D g, GridBoard<String> gridBoard, int x, int y,
                                                          GraphBoard graphBoard) {
        int width = gridBoard.getWidth() * defaultItemSize;
        int height = gridBoard.getHeight() * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                drawCell(g, gridBoard.getElement(j, i), j, i, gridBoard.getWidth(), gridBoard.getHeight(), graphBoard,
                        x, y);
            }
        }
    }

    private static void drawCell(Graphics2D g, String element, int x, int y, int gridWidth, int gridHeight, GraphBoard graph,
                                 int offsetX, int offsetY) {
        int xC = offsetX + x * defaultItemSize;
        int yC = offsetY + y * defaultItemSize;

        // Paint cell background
        g.setColor(colorMap.get(element));
        g.fillRect(xC, yC, defaultItemSize, defaultItemSize);
        g.setColor(Color.black);
        g.drawRect(xC, yC, defaultItemSize, defaultItemSize);

        // Find connectivity in the graph and draw borders to the cell where connection doesn't exist
        BoardNode bn = graph.getNodeByProperty(coordinateHash, new PropertyVector2D("coordinates", new Vector2D(x, y)));
        if (bn != null) {

            Stroke s = g.getStroke();
            g.setStroke(new BasicStroke(5));

            for (BoardNode n : bn.getNeighbours()) {
                if (n != null) {
                    Vector2D location = ((PropertyVector2D) n.getProperty(coordinateHash)).values;
                    // Draw line between these two if orthogonal neighbours
                    if (location.getX() - x == 0) {
                        // Vertical neighbours, draw horizontal line
                        if (location.getY() > y) {
                            // Neighbour is below, separation line is y + cell size
                            g.drawLine(xC, yC + defaultItemSize, xC + defaultItemSize, yC + defaultItemSize);
                        } else {
                            // Neighbour is above, separation line is y
                            g.drawLine(xC, yC, xC + defaultItemSize, yC);
                        }
                    } else if (location.getY() - y == 0) {
                        // Horizontal neighbours, draw vertical line
                        if (location.getX() > x) {
                            // Neighbour is to the right, separation line is x + cell size
                            g.drawLine(xC + defaultItemSize, yC, xC + defaultItemSize, yC + defaultItemSize);
                        } else {
                            // Neighbour is to the left, separation line is x
                            g.drawLine(xC, yC, xC, yC + defaultItemSize);
                        }
                    }
                }
            }

            g.setStroke(s);
        }
    }

}
