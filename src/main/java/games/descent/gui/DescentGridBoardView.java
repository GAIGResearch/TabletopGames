package games.descent.gui;

import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyColor;
import core.properties.PropertyString;
import core.properties.PropertyVector2D;
import games.descent.DescentGameState;
import games.descent.DescentTypes;
import games.descent.components.Figure;
import gui.views.ComponentView;
import utilities.Vector2D;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

import static core.AbstractGUI.defaultItemSize;
import static core.CoreConstants.colorHash;
import static core.CoreConstants.coordinateHash;
import static games.descent.DescentConstants.terrainHash;
import static utilities.Utils.getNeighbourhood;
import static utilities.Utils.stringToColor;

public class DescentGridBoardView extends ComponentView {

    public static HashMap<String, Color> colorMap = new HashMap<String, Color>() {{
        put("null", Color.gray);
        put(null, Color.gray);
        put("edge", Color.gray);
        put("plain", Color.white);
        put("block", Color.red);
        put("lava", Color.orange);
        put("water", Color.blue);
        put("hazard", Color.green);
        put("open", Color.lightGray);
        put("pit", Color.darkGray);
    }};

    private DescentGameState gameState;

    public DescentGridBoardView(GridBoard<String> gridBoard, DescentGameState gameState) {
        super(gridBoard, (gridBoard.getWidth()+1) * defaultItemSize, (gridBoard.getHeight()+1) * defaultItemSize);
        this.gameState = gameState;
    }

    public void updateGameState(DescentGameState gameState) {
        this.gameState = gameState;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoardWithGraphConnectivity((Graphics2D)g, (GridBoard<String>) component, 0, 0, gameState.getMasterGraph());

        // Draw figures
        for (int i = 1; i < gameState.getNPlayers(); i++) {
            Figure f = gameState.getHeroes().get(i-1);
            Vector2D loc = f.getLocation();
            g.setColor(stringToColor(((PropertyColor)f.getProperty(colorHash)).valueStr));
            g.fillOval(loc.getX() * defaultItemSize, loc.getY() * defaultItemSize, defaultItemSize, defaultItemSize);
            g.setColor(Color.black);
            g.drawOval(loc.getX() * defaultItemSize, loc.getY() * defaultItemSize, defaultItemSize, defaultItemSize);
        }
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

        // Find connectivity in the graph and draw borders to the cell where connection doesn't exist
        BoardNode bn = graph.getNodeByProperty(coordinateHash, new PropertyVector2D("coordinates", new Vector2D(x, y)));
        if (bn != null) {
            String terrain = ((PropertyString)bn.getProperty(terrainHash)).value;

            Stroke s = g.getStroke();

            if (DescentTypes.TerrainType.isWalkable(terrain)) {

                g.setColor(Color.black);
                g.drawRect(xC, yC, defaultItemSize, defaultItemSize);
                g.setStroke(new BasicStroke(5));

                List<Vector2D> neighbours = getNeighbourhood(x, y, gridWidth, gridHeight, false);
                for (Vector2D n : neighbours) {
                    BoardNode other = null;
                    for (BoardNode nn : bn.getNeighbours()) {
                        Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
                        if (location.equals(n)) {
                            other = nn;
                            break;
                        }
                    }
                    if (other == null) {
                        if (n.getX() - x == 0) {
                            // Vertical neighbours, draw horizontal line
                            if (n.getY() > y) {
                                // Neighbour is below, separation line is y + cell size
                                g.drawLine(xC, yC + defaultItemSize, xC + defaultItemSize, yC + defaultItemSize);
                            } else {
                                // Neighbour is above, separation line is y
                                g.drawLine(xC, yC, xC + defaultItemSize, yC);
                            }
                        } else if (n.getY() - y == 0) {
                            // Horizontal neighbours, draw vertical line
                            if (n.getX() > x) {
                                // Neighbour is to the right, separation line is x + cell size
                                g.drawLine(xC + defaultItemSize, yC, xC + defaultItemSize, yC + defaultItemSize);
                            } else {
                                // Neighbour is to the left, separation line is x
                                g.drawLine(xC, yC, xC, yC + defaultItemSize);
                            }
                        }
                    }
                }
            } else {
                if (terrain.equals("block")) {
                    g.setStroke(new BasicStroke(5));
                    g.setColor(Color.black);
                    g.drawRect(xC, yC, defaultItemSize, defaultItemSize);
                }
            }
            g.setStroke(s);

            // Draw underlying graph
            g.setColor(Color.green);
            for (BoardNode nn : bn.getNeighbours()) {
                Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
                int xC2 = offsetX + location.getX() * defaultItemSize;
                int yC2 = offsetY + location.getY() * defaultItemSize;

                g.drawLine(xC + defaultItemSize/2, yC + defaultItemSize/2, xC2 + defaultItemSize/2, yC2 + defaultItemSize/2);
            }
            g.setColor(Color.black);

        }
    }

}
