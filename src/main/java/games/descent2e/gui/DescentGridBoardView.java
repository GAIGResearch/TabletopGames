package games.descent2e.gui;

import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyColor;
import core.properties.PropertyString;
import core.properties.PropertyVector2D;
import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import gui.views.ComponentView;
import utilities.ImageIO;
import utilities.Pair;
import utilities.Vector2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static core.CoreConstants.*;
import static gui.AbstractGUIManager.defaultItemSize;
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

    public DescentGridBoardView(GridBoard gridBoard, DescentGameState gameState) {
        super(gridBoard, (gridBoard.getWidth()+1) * defaultItemSize, (gridBoard.getHeight()+1) * defaultItemSize);
        this.gameState = gameState;
    }

    public void updateGameState(DescentGameState gameState) {
        this.gameState = gameState;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawGridBoardWithGraphConnectivity((Graphics2D)g, (GridBoard) component, 0, 0, gameState.getGridReferences(), gameState.getTileReferences());

        // Draw heroes
        for (Figure f: gameState.getHeroes()) {
            Vector2D loc = f.getLocation();
            g.setColor(stringToColor(((PropertyColor)f.getProperty(colorHash)).valueStr));
            g.fillOval(loc.getX() * defaultItemSize, loc.getY() * defaultItemSize, defaultItemSize, defaultItemSize);
            g.setColor(Color.black);
            g.drawOval(loc.getX() * defaultItemSize, loc.getY() * defaultItemSize, defaultItemSize, defaultItemSize);
        }
        // Draw monsters
        for (ArrayList<Monster> monsterGroup: gameState.getMonsters()) {
            String dataPath = ((DescentParameters) gameState.getGameParameters()).dataPath + "img/";
            String path = ((PropertyString) monsterGroup.get(0).getProperty(imgHash)).value;

            for (Monster m: monsterGroup) {
                Vector2D loc = m.getLocation();
                int orientation = m.getOrientation();

                Pair<Integer, Integer> size = m.getSize();
                if (orientation % 2 == 1) {
                    size.swap();
                }

                String imagePath = dataPath;
                if (((PropertyColor) m.getProperty(colorHash)).valueStr.equals("red")) {
                    imagePath += path.replace(".png", "-master.png");
                } else {
                    imagePath += path;
                }
                Image img = ImageIO.GetInstance().getImage(imagePath);
                g.drawImage(img, loc.getX() * defaultItemSize, loc.getY() * defaultItemSize, size.a * defaultItemSize, size.b * defaultItemSize, null);
            }
        }

    }


    public void drawGridBoardWithGraphConnectivity(Graphics2D g, GridBoard gridBoard, int x, int y,
                                                          HashMap<String, HashSet<Vector2D>> gridReferences,
                                                          int[][] tileReferences) {
        int width = gridBoard.getWidth() * defaultItemSize;
        int height = gridBoard.getHeight() * defaultItemSize;

        // Draw background
        g.setColor(Color.lightGray);
        g.fillRect(x, y, width-1, height-1);
        g.setColor(Color.black);

        // Draw cells
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                drawCell(g, gridBoard.getElement(j, i), j, i, gridBoard.getWidth(), gridBoard.getHeight(), x, y);
            }
        }
        // Draw connectivity graph
        for (int i = 0; i < gridBoard.getHeight(); i++) {
            for (int j = 0; j < gridBoard.getWidth(); j++) {
                drawNeighbourConnections(g, gridBoard.getElement(j, i), j, i, x, y);
            }
        }

        // Draw grid references
        g.setColor(Color.black);
//        for (String tile: gridReferences.keySet()) {
//            for (Vector2D t: gridReferences.get(tile)) {
//                g.drawString(tile, t.getX()*defaultItemSize + 10, t.getY()*defaultItemSize + 10);
//            }
//        }

        // Draw tile references
//        for (int i = 0; i < gridBoard.getHeight(); i++) {
//            for (int j = 0; j < gridBoard.getWidth(); j++) {
//                g.drawString(""+tileReferences[i][j], j*defaultItemSize + 10, i*defaultItemSize + 10);
//            }
//        }
    }

    private void drawCell(Graphics2D g, BoardNode bn, int x, int y, int gridWidth, int gridHeight, int offsetX, int offsetY) {
        if (bn == null) return;

        int xC = offsetX + x * defaultItemSize;
        int yC = offsetY + y * defaultItemSize;

        // Paint cell background
        g.setColor(colorMap.get(bn.getComponentName()));
        g.fillRect(xC, yC, defaultItemSize, defaultItemSize);

        String terrain = bn.getComponentName();
        Stroke s = g.getStroke();

        if (DescentTypes.TerrainType.isWalkable(terrain)) {
            g.setColor(Color.black);
            g.drawRect(xC, yC, defaultItemSize, defaultItemSize);
            g.setStroke(new BasicStroke(5));

            // Find connectivity in the graph and draw borders to the cell where connection doesn't exist
            List<Vector2D> neighbourCells = getNeighbourhood(x, y, gridWidth, gridHeight, false);
            // Explore all neighbourhood of this cell
            for (Vector2D n : neighbourCells) {

                // Check if this node is a connected neighbour
                boolean connected = false;
                for (int nnid : bn.getNeighbours()) {
                    BoardNode nn = (BoardNode) gameState.getComponentById(nnid);
                    if (nn == null) continue;
                    Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
                    if (location.equals(n)) {
                        connected = true;
                        break;
                    }
                }
                if (!connected) {
                    // Not a connection between these neighbours, drawing a thick black line on the edge to indicate this
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
            // Bocked terrain can never be occupied, not connected to anything
            if (terrain.equals("block")) {
                g.setStroke(new BasicStroke(5));
                g.setColor(Color.black);
                g.drawRect(xC, yC, defaultItemSize, defaultItemSize);
            }
        }
        g.setStroke(s);
    }

    private void drawNeighbourConnections(Graphics2D g, BoardNode bn, int x, int y, int offsetX, int offsetY) {
        if (bn == null) return;
        int xC = offsetX + x * defaultItemSize;
        int yC = offsetY + y * defaultItemSize;

        // Draw underlying graph
        g.setColor(Color.green);
        for (int nnid : bn.getNeighbours()) {
            BoardNode nn = (BoardNode) gameState.getComponentById(nnid);
            if (nn == null) continue;
            Vector2D location = ((PropertyVector2D) nn.getProperty(coordinateHash)).values;
            int xC2 = offsetX + location.getX() * defaultItemSize;
            int yC2 = offsetY + location.getY() * defaultItemSize;

            g.drawLine(xC + defaultItemSize/2, yC + defaultItemSize/2, xC2 + defaultItemSize/2, yC2 + defaultItemSize/2);
        }
        g.setColor(Color.black);
    }

}
