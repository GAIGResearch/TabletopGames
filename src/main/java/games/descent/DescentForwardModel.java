package games.descent;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyStringArray;
import utilities.Vector2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static core.CoreConstants.neighbourHash;
import static core.CoreConstants.orientationHash;
import static games.descent.DescentConstants.connectionHash;

public class DescentForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DescentGameState dgs = (DescentGameState) firstState;

        DescentGameData _data = dgs.getData();
        // 1. Read the graph board configuration for the master grid board
        GraphBoard config = _data.findGraphBoard("board1");

        // 2. Read all necessary tiles, which are all grid boards. Keep in a list.
        dgs.tiles = new HashMap<>();
        for (BoardNode bn: config.getBoardNodes()) {
            GridBoard tile = _data.findGridBoard(bn.getComponentName());
            dgs.tiles.put(bn.getComponentID(), tile);
        }

        // 3. Put together the master grid board
        // Find maximum board width and height, if all were put together side by side
        int width = 0;
        int height = 0;
        for (BoardNode bn: config.getBoardNodes()) {
            // Find width of this tile, according to orientation
            GridBoard tile = dgs.tiles.get(bn.getComponentID());
            int orientation = ((PropertyInt)bn.getProperty(orientationHash)).value;
            if (orientation % 2 == 0) {
                width += tile.getWidth();
                height += tile.getHeight();
            } else {
                width += tile.getHeight();
                height += tile.getWidth();
            }
        }

        // First tile will be in the center, board could expand in more directions
        width *= 2;
        height *= 2;

        // Create big board
        String[][] board = new String[height][width];
        dgs.tileReferences = new int[height][width];
        HashSet<BoardNode> drawn = new HashSet<>();

        BoardNode bn0 = config.getBoardNodes().get(0);  // Assumes all tiles in this board are connected
        GridBoard tile = dgs.tiles.get(bn0.getComponentID());
        int orientation = ((PropertyInt)bn0.getProperty(orientationHash)).value;
        String[][] rotated = (String[][]) tile.rotate(orientation);
        int startX = width / 2 - rotated[0].length/2;
        int startY = height / 2 - rotated.length/2;
        Rectangle bounds = addTilesToBoard(bn0, startX, startY, board, dgs.tiles, dgs.tileReferences, drawn,
                startX, startY, startX + rotated[0].length, startY + rotated.length);

        // TODO: trim the resulting board to remove excess border of nulls according to 'bounds' rectangle

        dgs.masterBoard = new GridBoard<>(board, String.class);

        // TODO initial setup
    }

    private Rectangle addTilesToBoard(BoardNode bn, int x, int y, String[][] board, HashMap<Integer, GridBoard> tiles,
                                      int[][] tileReferences, HashSet<BoardNode> drawn,
                                      int minX, int minY, int maxX, int maxY) {
        if (!drawn.contains(bn)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = tiles.get(bn.getComponentID());
            String[][] tileGrid = (String[][]) tile.rotate(((PropertyInt)bn.getProperty(orientationHash)).value);
            int height = tileGrid.length;
            int width = tileGrid[0].length;

            HashMap<String, ArrayList<Vector2D>> openings = findOpenings(tileGrid);

            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    board[i][j] = tileGrid[i-y][j-x];
                    tileReferences[i][j] = tile.getComponentID();
                }
            }
            drawn.add(bn);

            // Draw neighbours
            for (BoardNode neighbour: bn.getNeighbours()) {

                // Find location to start drawing neighbour
                Vector2D connectionToNeighbour = findConnection(bn, neighbour, openings);

                if (connectionToNeighbour != null) {
                    // Find orientation and opening connection from neighbour, generate top-left corner of neighbour from that
                    GridBoard tileN = tiles.get(neighbour.getComponentID());
                    String[][] tileGridN = (String[][]) tileN.rotate(((PropertyInt)neighbour.getProperty(orientationHash)).value);

                    // Find open spots on the tile
                    HashMap<String, ArrayList<Vector2D>> openings2 = findOpenings(tileGridN);

                    // Find location to start drawing neighbour
                    Vector2D connectionFromNeighbour = findConnection(neighbour, bn, openings2);

                    if (connectionFromNeighbour != null) {
                        Vector2D topLeftCorner = new Vector2D(connectionToNeighbour.getX() - connectionFromNeighbour.getX(),
                                connectionToNeighbour.getY() - connectionFromNeighbour.getY());

                        // Update area bounds
                        if (topLeftCorner.getX() < minX) minX = topLeftCorner.getX();
                        if (topLeftCorner.getY() < minY) minY = topLeftCorner.getY();
                        if (topLeftCorner.getX() + tileGridN[0].length > maxX) maxX = topLeftCorner.getX() + tileGridN[0].length;
                        if (topLeftCorner.getY() + tileGridN.length > maxY) maxY = topLeftCorner.getY() + tileGridN.length;

                        // Draw neighbour recursively
                        addTilesToBoard(neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tiles, tileReferences, drawn,
                                minX, minY, maxX, maxY);
                    }
                }
            }
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private Vector2D findConnection(BoardNode from, BoardNode to, HashMap<String, ArrayList<Vector2D>> openings) {
        String[] neighbours = ((PropertyStringArray) from.getProperty(neighbourHash)).getValues();
        String[] connections = ((PropertyStringArray) from.getProperty(connectionHash)).getValues();

        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].equalsIgnoreCase(to.getComponentName())) {
                String conn = connections[i];

                String side = conn.split("-")[0];
                int countFromTop = Integer.parseInt(conn.split("-")[1]);
                if (openings.containsKey(side)) {
                    if (countFromTop >= 0 && countFromTop < openings.get(side).size()) {
                        return openings.get(side).get(countFromTop);
                    }
                }
                break;
            }
        }
        return null;
    }

    private HashMap<String, ArrayList<Vector2D>> findOpenings(String[][] tileGrid) {
        int height = tileGrid.length;
        int width = tileGrid[0].length;

        HashMap<String, ArrayList<Vector2D>> openings = new HashMap<>();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (tileGrid[i][j].equalsIgnoreCase("open")) {
                    // Which side are we on?
                    // TODO: corners, non-grid tiles
                    String side;
                    if (i == 0) { // top
                        side = "N";
                    } else if (i == height - 1) {  // bottom
                        side = "S";
                    } else if (j == 0) {
                        side = "W";
                    } else {
                        side = "E";
                    }
                    if (!openings.containsKey(side)) {
                        openings.put(side, new ArrayList<>());
                    }
                    openings.get(side).add(new Vector2D(j, i));
                }
            }
        }
        return openings;
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        if (checkEndOfGame()) return;
        currentState.getTurnOrder().endPlayerTurn(currentState);
        // TODO
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());
        // TODO
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new DescentForwardModel();
    }

    private boolean checkEndOfGame() {
        // TODO
        return false;
    }
}
