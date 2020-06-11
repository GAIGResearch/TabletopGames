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

import games.descent.DescentTypes.*;

import static core.CoreConstants.neighbourHash;
import static core.CoreConstants.orientationHash;
import static games.descent.DescentConstants.connectionHash;
import static utilities.Utils.getNeighbourhood;

public class DescentForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DescentGameState dgs = (DescentGameState) firstState;
        DescentGameData _data = dgs.getData();

        // TODO: epic play options (pg 19)

        // 1. Get campaign from game parameters, load all the necessary information
        Campaign campaign = ((DescentParameters)dgs.getGameParameters()).campaign;
//        _data.getCampaign();

        // First player is always the overlord

        // 5. Player setup phase interrupts, after which setup continues:
        // Player chooses hero, class, skills
        // Set up player tokens

        // 2. Set up the first step of the campaign: either a quest, or a world map
        // Overlord chooses monster groups
        // Shuffle overlord deck and give overlord nPlayers cards.
        // Separate shop items by acts, shuffle.
        // Separate monster and lieutenent cards into 2 acts.
        // Set up dice

        // 4. Shuffle search cards deck


        // 1. Read the graph board configuration for the master grid board
        GraphBoard config = _data.findGraphBoard("board2");

        // 2. Read all necessary tiles, which are all grid boards. Keep in a list.
        dgs.tiles = new HashMap<>();
        for (BoardNode bn: config.getBoardNodes()) {
            String name = bn.getComponentName();
            if (name.contains("-")) {  // There may be multiples of one tile in the board, which follow format "tilename-#"
                name = name.split("-")[0];
            }
            GridBoard tile = _data.findGridBoard(name);
            if (tile != null) {
                dgs.tiles.put(bn.getComponentID(), tile);
            }
        }

        // 3. Put together the master grid board
        // Find maximum board width and height, if all were put together side by side
        int width = 0;
        int height = 0;
        for (BoardNode bn: config.getBoardNodes()) {
            // Find width of this tile, according to orientation
            GridBoard tile = dgs.tiles.get(bn.getComponentID());
            if (tile != null) {
                int orientation = ((PropertyInt) bn.getProperty(orientationHash)).value;
                if (orientation % 2 == 0) {
                    width += tile.getWidth();
                    height += tile.getHeight();
                } else {
                    width += tile.getHeight();
                    height += tile.getWidth();
                }
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

        BoardNode bn0 = null;
        for (BoardNode b: config.getBoardNodes()) {
            if (b != null) {
                GridBoard tile = dgs.tiles.get(b.getComponentID());
                if (tile != null) {
                    bn0 = b;
                    break;
                }
            }
        }
        if (bn0 != null) {
            GridBoard tile = dgs.tiles.get(bn0.getComponentID());
            int orientation = ((PropertyInt) bn0.getProperty(orientationHash)).value;
            String[][] rotated = (String[][]) tile.rotate(orientation);
            int startX = width / 2 - rotated[0].length / 2;
            int startY = height / 2 - rotated.length / 2;
            Rectangle bounds = new Rectangle(startX, startY, rotated[0].length, rotated.length);
            addTilesToBoard(bn0, startX, startY, board, null, dgs.tiles, dgs.tileReferences, drawn, neighbours, bounds);

            // Trim the resulting board and tile references to remove excess border of nulls according to 'bounds' rectangle
            String[][] trimBoard = new String[bounds.height][bounds.width];
            int[][] trimTileRef = new int[bounds.height][bounds.width];
            for (int i = 0; i < bounds.height; i++) {
                if (bounds.width >= 0) System.arraycopy(board[i + bounds.y], bounds.x, trimBoard[i], 0, bounds.width);
                if (bounds.width >= 0)
                    System.arraycopy(dgs.tileReferences[i + bounds.y], bounds.x, trimTileRef[i], 0, bounds.width);
            }
            dgs.tileReferences = trimTileRef;
            // Also trim neighbour records
            for (Pair<Vector2D, Vector2D> p : neighbours) {
                p.a.subtract(bounds.x, bounds.y);
                p.b.subtract(bounds.x, bounds.y);
            }

            // This is the master board!
            dgs.masterBoard = new GridBoard<>(trimBoard, String.class);
            dgs.masterGraph = dgs.masterBoard.toGraphBoard(neighbours);

            // TODO initial setup
        } else {
            System.out.println("Tiles for the map not found");
        }
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        if (checkEndOfGame()) return;
        currentState.getTurnOrder().endPlayerTurn(currentState);
        // TODO

        // Transitioning between encounters:
        // Set up campaign phase
        // receive gold from search cards and return cards to the deck.
        // recover all damage and fatigue, discard conditions and effects
        // receive quest reward (1XP per hero + bonus from quest)
        // shopping (if right after interlude, can buy any act 1 cards, then remove these from game)
        // spend XP points for skills
        // choose next quest (winner chooses)
        // set up next quest

        // choosing interlude: the heroes pick if they won >= 2 act 1 quests, overlord picks if they won >=2 quests

        // TODO: in 2-hero games, free regular attack action each turn or recover 2 damage.
        // TODO: 2 player games, with 2 heroes for one, and the other the overlord.
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

    /**
     * Recursively adds tiles to the board, iterating through all neighbours and updating references from grid
     * to tiles, list of valid neighbours for movement graph according to Descent movement rules, and bounds for
     * the resulting grid of the master board (with all tiles put together).
     * @param bn - board node representing tile to add to board
     * @param x - top-left x-coordinate for this tile's location in the master board
     * @param y - top-left y-coordinate for this tile's location in the master board
     * @param board - the master grid board representation
     * @param tileGrid - grid representation of tile to add to board (possibly a trimmed version from its corresponding
     *                 object in the "tiles" map to fit together with existing board)
     * @param tiles - mapping from board node component ID, to GridBoard object representing the tile
     * @param tileReferences - references from each cell in the grid to the component ID of the GridBoard representing
     *                       the tile at that location
     * @param drawn - a list of board nodes already drawn, to avoid drawing twice during recursive calls.
     * @param neighbours - a list of pairs of neighbouring cells
     * @param bounds - bounds of contents of the master grid board
     */
    private void addTilesToBoard(BoardNode bn, int x, int y, String[][] board,
                                 String[][] tileGrid,
                                 HashMap<Integer, GridBoard> tiles,
                                 int[][] tileReferences, HashSet<BoardNode> drawn,
                                 ArrayList<Pair<Vector2D, Vector2D>> neighbours,
                                 Rectangle bounds) {
        if (!drawn.contains(bn)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = tiles.get(bn.getComponentID());
            if (tileGrid == null) {
                tileGrid = (String[][]) tile.rotate(((PropertyInt) bn.getProperty(orientationHash)).value);
            }
            int height = tileGrid.length;
            int width = tileGrid[0].length;

            // Connect the new tile with current board. Tile overlapping here has 8-way connectivity,
            // as do its neighbours on new tile
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    if (board[i][j] != null && board[i][j].equalsIgnoreCase("open")) {
                        Vector2D point = new Vector2D(j, i);

                        // Add connections from this point to points on the master board
                        List<Vector2D> boardNs = getNeighbourhood(j, i, board[0].length, board.length, true);
                        if (TerrainType.isWalkable(tileGrid[point.getY() - y][point.getX() - x])) {
                            // Connect each cell that can connect to the master board with all possible connections
                            for (Vector2D n2 : boardNs) {
                                if (TerrainType.isWalkable(board[n2.getY()][n2.getX()]) &&
                                        !n2.equals(point)) {
                                    if (Math.abs(point.getY() - n2.getY()) <= 1 && Math.abs(point.getX() - n2.getX()) <= 1) {
                                        neighbours.add(new Pair<>(point.copy(), n2.copy()));
                                    }
                                }
                            }
                        }

                        // Add connections from connecting point on the master board to this tile, the orthogonal neighbour that's an inside piece
                        List<Vector2D> possible = getNeighbourhood(j, i, board[0].length, board.length, false);
                        Vector2D other = null;
                        for (Vector2D p: possible) {
                            if (TerrainType.isInsideTile(board[p.getY()][p.getX()])) {
                                other = p;
                                break;
                            }
                        }

                        if (other != null) {
                            List<Vector2D> tileNs = getNeighbourhood(j - x, i - y, width, height, true);
                            // Connect each cell that can connect to the master board with all possible connections
                            for (Vector2D n2 : tileNs) {
                                Vector2D pointInBoard = new Vector2D(n2.getX() + x, n2.getY() + y);
                                if (TerrainType.isWalkable(tileGrid[n2.getY()][n2.getX()]) &&
                                        !pointInBoard.equals(other)) {
                                    if (Math.abs(other.getY() - pointInBoard.getY()) <= 1 && Math.abs(other.getX() - pointInBoard.getX()) <= 1) {
                                        neighbours.add(new Pair<>(other.copy(), pointInBoard.copy()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add connections between all tiles just placed, unless blocked (no blocked tiles are connected)
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (TerrainType.isWalkable(tileGrid[i][j])) {
                        List<Vector2D> ns = getNeighbourhood(j, i, width, height, true);
                        for (Vector2D nn : ns) {
                            if (TerrainType.isWalkable(tileGrid[nn.getY()][nn.getX()]) &&
                                    !(nn.getX() == j && nn.getY() == i)) {
                                neighbours.add(new Pair<>(new Vector2D(j + x, i + y), new Vector2D(nn.getX() + x, nn.getY() + y)));
                            }
                        }
                    }
                }
            }

            // Add cells from new tile to the master board
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    board[i][j] = tileGrid[i-y][j-x];
                    tileReferences[i][j] = tile.getComponentID();
                }
            }

            // This tile was drawn
            drawn.add(bn);

            // Draw neighbours
            for (BoardNode neighbour: bn.getNeighbours()) {

                // Find location to start drawing neighbour
                Pair<String, Vector2D> connectionToNeighbour = findConnection(bn, neighbour, findOpenings(tileGrid));

                if (connectionToNeighbour != null) {
                    connectionToNeighbour.b.add(x, y);
                    // Find orientation and opening connection from neighbour, generate top-left corner of neighbour from that
                    GridBoard tileN = tiles.get(neighbour.getComponentID());
                    if (tileN != null) {
                        String[][] tileGridN = (String[][]) tileN.rotate(((PropertyInt) neighbour.getProperty(orientationHash)).value);

                        // Find location to start drawing neighbour
                        Pair<String, Vector2D> conn2 = findConnection(neighbour, bn, findOpenings(tileGridN));

                        int w = tileGridN[0].length;
                        int h = tileGridN.length;

                        if (conn2 != null) {
                            String side = conn2.a;
                            Vector2D connectionFromNeighbour = conn2.b;
                            if (side.equalsIgnoreCase("W")) {
                                // Remove first column
                                String[][] tileGridNTrim = new String[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 1, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("E")) {
                                connectionFromNeighbour.subtract(1, 0);
                                // Remove last column
                                String[][] tileGridNTrim = new String[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("N")) {
                                // Remove first row
                                String[][] tileGridNTrim = new String[h - 1][w];
                                for (int i = 1; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i - 1], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            } else {
                                connectionFromNeighbour.subtract(0, 1);
                                // Remove last row
                                String[][] tileGridNTrim = new String[h - 1][w];
                                for (int i = 0; i < h - 1; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            }
                            Vector2D topLeftCorner = new Vector2D(connectionToNeighbour.b.getX() - connectionFromNeighbour.getX(),
                                    connectionToNeighbour.b.getY() - connectionFromNeighbour.getY());

                            // Update area bounds
                            if (topLeftCorner.getX() < bounds.x) bounds.x = topLeftCorner.getX();
                            if (topLeftCorner.getY() < bounds.y) bounds.y = topLeftCorner.getY();
                            int deltaMaxX = (int) (topLeftCorner.getX() + tileGridN[0].length - bounds.getMaxX());
                            if (deltaMaxX > 0) bounds.width += deltaMaxX;
                            int deltaMaxY = (int) (topLeftCorner.getY() + tileGridN.length - bounds.getMaxY());
                            if (deltaMaxY > 0) bounds.height += deltaMaxY;

                            // Draw neighbour recursively
                            addTilesToBoard(neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tileGridN,
                                    tiles, tileReferences, drawn, neighbours, bounds);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a connection between two boardnodes representing tiles in the game (i.e. where the 2 tiles should be
     * connecting according to board configuration)
     * @param from - origin board node to find connection from
     * @param to - board node to find connection to
     * @param openings - list of openings for the origin board node
     * @return - a pair of side, and location (in tile space) of openings that would connect to the given tile as required
     */
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

    /**
     * Finds coordinates (in tile space) for where openings on all sides (top-left locations).
     * // TODO: assumes all openings 2-tile wide + no openings are next to each other.
     * @param tileGrid - grid to look for openings in
     * @return - Mapping from side (N, S, W, E) to a list of openings on that particular side.
     */
    private HashMap<String, ArrayList<Vector2D>> findOpenings(String[][] tileGrid) {
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
}
