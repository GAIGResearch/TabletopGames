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
import utilities.Pair;
import utilities.Vector2D;

import java.awt.*;
import java.util.*;
import java.util.List;

import static core.CoreConstants.neighbourHash;
import static core.CoreConstants.orientationHash;
import static games.descent.DescentConstants.connectionHash;
import static utilities.Utils.getNeighbourhood;

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
            String name = bn.getComponentName();
            if (name.contains("-")) {  // There may be multiples of one tile in the board, which follow format "tilename-#"
                name = name.split("-")[0];
            }
            GridBoard tile = _data.findGridBoard(name);
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
        ArrayList<Pair<Vector2D, Vector2D>> neighbours = new ArrayList<>();  // Holds neighbouring cells information

        BoardNode bn0 = config.getBoardNodes().get(0);  // Assumes all tiles in this board are connected
        GridBoard tile = dgs.tiles.get(bn0.getComponentID());
        int orientation = ((PropertyInt)bn0.getProperty(orientationHash)).value;
        String[][] rotated = (String[][]) tile.rotate(orientation);
        int startX = width / 2 - rotated[0].length/2;
        int startY = height / 2 - rotated.length/2;
        Rectangle bounds = addTilesToBoard(bn0, startX, startY, board, null, dgs.tiles, dgs.tileReferences,
                drawn, neighbours,
                startX, startY, startX + rotated[0].length, startY + rotated.length);

        // Trim the resulting board and tile references to remove excess border of nulls according to 'bounds' rectangle
        String[][] trimBoard = new String[bounds.height][bounds.width];
        int[][] trimTileRef = new int[bounds.height][bounds.width];
        for (int i = 0; i < bounds.height; i++) {
            if (bounds.width >= 0) System.arraycopy(board[i + bounds.y], bounds.x, trimBoard[i], 0, bounds.width);
            if (bounds.width >= 0) System.arraycopy(dgs.tileReferences[i + bounds.y], bounds.x, trimTileRef[i], 0, bounds.width);
        }
        dgs.tileReferences = trimTileRef;
        // Also trim neighbour records
        for (Pair<Vector2D, Vector2D> p: neighbours) {
            p.a.subtract(bounds.x, bounds.y);
            p.b.subtract(bounds.x, bounds.y);
        }

        // This is the master board!
        dgs.masterBoard = new GridBoard<>(trimBoard, String.class);
        dgs.masterGraph = dgs.masterBoard.toGraphBoard(neighbours, true);

        // TODO initial setup
    }

    private Rectangle addTilesToBoard(BoardNode bn, int x, int y, String[][] board,
                                      String[][] tileGrid,
                                      HashMap<Integer, GridBoard> tiles,
                                      int[][] tileReferences, HashSet<BoardNode> drawn,
                                      ArrayList<Pair<Vector2D, Vector2D>> neighbours,
                                      int minX, int minY, int maxX, int maxY) {
        if (!drawn.contains(bn)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = tiles.get(bn.getComponentID());
            if (tileGrid == null) {
                tileGrid = (String[][]) tile.rotate(((PropertyInt) bn.getProperty(orientationHash)).value);
            }
            int height = tileGrid.length;
            int width = tileGrid[0].length;

            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    if (board[i][j] != null && board[i][j].equalsIgnoreCase("open")) {
                        // Connect the new tile with current board. Tile overlapping here has 8-way connectivity,
                        // as do its neighbours on new tile
                        List<Vector2D> boardNs = getNeighbourhood(j, i, board[0].length, board.length, true);
                        List<Vector2D> newTilesNs = getNeighbourhood(j - x, i - y, width, height, true);
                        newTilesNs.add(new Vector2D(j-x, i-y));
                        for (Vector2D n1: newTilesNs) {
                            if (DescentConstants.TerrainType.isWalkable(tileGrid[n1.getY()][n1.getX()])) {
                                Vector2D n1InBoard = new Vector2D(n1.getX() + x, n1.getY() + y);
                                // Connect each tile that can connect to the new board with all possible connections
                                for (Vector2D n2 : boardNs) {
                                    if (DescentConstants.TerrainType.isWalkable(board[n2.getY()][n2.getX()]) &&
                                    Math.abs(n1InBoard.getY() - n2.getY()) <= 1 && Math.abs(n1InBoard.getX() - n2.getX()) <= 1) {
                                        neighbours.add(new Pair<>(n1InBoard.copy(), n2.copy()));
                                    }
                                }
                            }
                        }
                    }

                    // Add cells from new tile to the master board
                    board[i][j] = tileGrid[i-y][j-x];
                    tileReferences[i][j] = tile.getComponentID();
                }
            }
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    // Add connections between all tiles just placed, unless blocked (no blocked tiles are connected)
                    if (DescentConstants.TerrainType.isWalkable(board[j][i])) {
                        Vector2D thisNode = new Vector2D(j, i);
                        List<Vector2D> ns = getNeighbourhood(j, i, board[0].length, board.length, true);
                        for (Vector2D n : ns) {
                            if (DescentConstants.TerrainType.isWalkable(board[n.getY()][n.getX()])) {
                                neighbours.add(new Pair<>(thisNode.copy(), n.copy()));
                            }
                        }
                    }
                }
            }
            drawn.add(bn);

            // Draw neighbours
            for (BoardNode neighbour: bn.getNeighbours()) {

                // Find location to start drawing neighbour
                Pair<String, Vector2D> connectionToNeighbour = findConnection(bn, neighbour, findOpenings(tileGrid));

                if (connectionToNeighbour != null) {
                    connectionToNeighbour.b.add(x, y);
                    // Find orientation and opening connection from neighbour, generate top-left corner of neighbour from that
                    GridBoard tileN = tiles.get(neighbour.getComponentID());
                    String[][] tileGridN = (String[][]) tileN.rotate(((PropertyInt)neighbour.getProperty(orientationHash)).value);

                    // Find location to start drawing neighbour
                    Pair<String, Vector2D> conn2 = findConnection(neighbour, bn, findOpenings(tileGridN));

                    int w = tileGridN[0].length;
                    int h = tileGridN.length;

                    if (conn2 != null) {
                        String side = conn2.a;
                        Vector2D connectionFromNeighbour = conn2.b;
                        if (side.equalsIgnoreCase("W")) {
                            // Remove first column
                            String[][] tileGridNTrim = new String[h][w-1];
                            for (int i = 0; i < h; i++) {
                                System.arraycopy(tileGridN[i], 1, tileGridNTrim[i], 0, w - 1);
                            }
                            tileGridN = tileGridNTrim;
                        } else if (side.equalsIgnoreCase("E")) {
                            connectionFromNeighbour.subtract(1, 0);
                            // Remove last column
                            String[][] tileGridNTrim = new String[h][w-1];
                            for (int i = 0; i < h; i++) {
                                System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w - 1);
                            }
                            tileGridN = tileGridNTrim;
                        } else if (side.equalsIgnoreCase("N")) {
                            // Remove first row
                            String[][] tileGridNTrim = new String[h-1][w];
                            for (int i = 1; i < h; i++) {
                                System.arraycopy(tileGridN[i], 0, tileGridNTrim[i - 1], 0, w);
                            }
                            tileGridN = tileGridNTrim;
                        } else {
                            connectionFromNeighbour.subtract(0, 1);
                            // Remove last row
                            String[][] tileGridNTrim = new String[h-1][w];
                            for (int i = 0; i < h-1; i++) {
                                System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w);
                            }
                            tileGridN = tileGridNTrim;
                        }
                        Vector2D topLeftCorner = new Vector2D(connectionToNeighbour.b.getX() - connectionFromNeighbour.getX(),
                                connectionToNeighbour.b.getY() - connectionFromNeighbour.getY());

                        // Update area bounds
                        if (topLeftCorner.getX() < minX) minX = topLeftCorner.getX();
                        if (topLeftCorner.getY() < minY) minY = topLeftCorner.getY();
                        if (topLeftCorner.getX() + tileGridN[0].length > maxX) maxX = topLeftCorner.getX() + tileGridN[0].length;
                        if (topLeftCorner.getY() + tileGridN.length > maxY) maxY = topLeftCorner.getY() + tileGridN.length;

                        // Draw neighbour recursively
                        addTilesToBoard(neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tileGridN,
                                tiles, tileReferences, drawn, neighbours,
                                minX, minY, maxX, maxY);
                    }
                }
            }
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private Pair<String, Vector2D> findConnection(BoardNode from, BoardNode to, HashMap<String, ArrayList<Vector2D>> openings) {
        String[] neighbours = ((PropertyStringArray) from.getProperty(neighbourHash)).getValues();
        String[] connections = ((PropertyStringArray) from.getProperty(connectionHash)).getValues();

        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].equalsIgnoreCase(to.getComponentName())) {
                String conn = connections[i];

                String side = conn.split("-")[0];
                int countFromTop = Integer.parseInt(conn.split("-")[1]);
                if (openings.containsKey(side)) {
                    if (countFromTop >= 0 && countFromTop < openings.get(side).size()) {
                        return new Pair(side, openings.get(side).get(countFromTop));
                    }
                }
                break;
            }
        }
        return null;
    }

    private HashMap<String, ArrayList<Vector2D>> findOpenings(String[][] tileGrid) {
        // TODO: assumes all openings 2-tile wide + no openings are next to each other.
        int height = tileGrid.length;
        int width = tileGrid[0].length;

        HashMap<String, ArrayList<Vector2D>> openings = new HashMap<>();
        // TOP, check each column, stop at the first encounter in each column.
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++) {
                if (tileGrid[i][j].equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile above
                    if (i == 0 || tileGrid[i-1][j].equalsIgnoreCase("null") ||
                            tileGrid[i-1][j].equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || !tileGrid[i][j-1].equalsIgnoreCase("open")) &&
                                (j < width-1 && tileGrid[i][j+1].equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("N")) {
                                openings.put("N", new ArrayList<>());
                            }
                            openings.get("N").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // BOTTOM, check each column, stop at the first encounter in each column (read from bottom to top).
        for (int j = 0; j < width; j++) {
            for (int i = height-1; i >= 0; i--) {
                if (tileGrid[i][j].equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile below
                    if (i == height-1 || tileGrid[i+1][j].equalsIgnoreCase("null") ||
                            tileGrid[i+1][j].equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || !tileGrid[i][j-1].equalsIgnoreCase("open")) &&
                                (j < width-1 && tileGrid[i][j+1].equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("S")) {
                                openings.put("S", new ArrayList<>());
                            }
                            openings.get("S").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // LEFT, check each row, stop at the first encounter in each row.
        for (int i = 0; i < height; i++){
            for (int j = 0; j < width; j++) {
                if (tileGrid[i][j].equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the left
                    if (j == 0 || tileGrid[i][j-1].equalsIgnoreCase("null") ||
                            tileGrid[i][j-1].equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i-1][j].equalsIgnoreCase("open")) &&
                                (i < height-1 && tileGrid[i+1][j].equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("W")) {
                                openings.put("W", new ArrayList<>());
                            }
                            openings.get("W").add(new Vector2D(j, i));
                            break;
                        }
                    }
                }
            }
        }
        // RIGHT, check each row, stop at the first encounter in each row (read from right to left).
        for (int i = 0; i < height; i++){
            for (int j = width-1; j >= 0; j--) {
                if (tileGrid[i][j].equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the right
                    if (j == width-1 || tileGrid[i][j+1].equalsIgnoreCase("null") ||
                            tileGrid[i][j+1].equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i-1][j].equalsIgnoreCase("open")) &&
                                (i < height-1 && tileGrid[i+1][j].equalsIgnoreCase("open"))) {
                            if (!openings.containsKey("E")) {
                                openings.put("E", new ArrayList<>());
                            }
                            openings.get("E").add(new Vector2D(j, i));
                            break;
                        }
                    }
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
