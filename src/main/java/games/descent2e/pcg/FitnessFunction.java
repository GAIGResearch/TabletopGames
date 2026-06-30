package games.descent2e.pcg;

import com.google.apps.card.v1.Grid;
import core.components.BoardNode;
import core.components.Component;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyIntArray;
import core.properties.PropertyStringArray;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import org.apache.hadoop.yarn.state.Graph;
import utilities.Hash;
import utilities.Pair;
import utilities.Vector2D;

import java.awt.*;
import java.util.*;
import java.util.List;

import static core.CoreConstants.neighbourHash;
import static core.CoreConstants.orientationHash;
import static games.descent2e.DescentConstants.connectionHash;
import static utilities.Utils.getNeighbourhood;

public class FitnessFunction {
    public static final int W_CONNECTED = 1;
    public static final int W_GEOMETRY = 1;
    public static final int W_REPEATS = 1;
    public static final int W_SPAWNING = 1;
    public static final int W_CONSISTENCY = 1;
    public static final int W_SIZE = 1;
    public static final int W_GROUP = 1;
    public static final int W_HEALTH = 1;
    public static final int W_COMPLEXITY = 1;
    public static final int W_RULES = 1;

    public static final int IDEAL_SIZE = 166;
    public static final int IDEAL_GROUP = 5;
    public static final float IDEAL_HEALTH = 5.872f;
    public static final int IDEAL_COMPLEXITY = 1;
    public static final int IDEAL_RULES = 1;

    public static float fitness(List<Float> scores) {
        float fitness = 0f;

        fitness += W_CONNECTED * (1 / scores.get(0));
        fitness += W_GEOMETRY * scores.get(1);
        fitness += W_REPEATS * scores.get(2);
        fitness += W_SPAWNING * scores.get(3);
        fitness += W_CONSISTENCY * scores.get(4);

        fitness += W_SIZE * (1f - (Math.abs(IDEAL_SIZE - scores.get(5)) / IDEAL_SIZE));
        fitness += W_GROUP * (1f - (Math.abs(IDEAL_GROUP - scores.get(6)) / IDEAL_GROUP));
        fitness += W_HEALTH * (1f - (Math.abs(IDEAL_HEALTH - scores.get(7)) / IDEAL_HEALTH));

        fitness += W_COMPLEXITY * (1f - (Math.abs(IDEAL_COMPLEXITY - scores.get(8)) / IDEAL_COMPLEXITY));
        fitness += W_RULES * (1f - (Math.abs(IDEAL_RULES - scores.get(9)) / IDEAL_RULES));

        return fitness;
    }

    public static int connectedness(GraphBoard board) {
        int islands = 0;

        BoardNode[] nodes = board.getBoardNodeMap().values().toArray(new BoardNode[0]);

        List<String> tiles = new ArrayList<>();
        for (BoardNode n : nodes) {
            if (tiles.contains(n.getComponentName())) continue;
            islands++;
            tiles.add(n.getComponentName());
            for (BoardNode neighbour : n.getNeighbours().keySet()) {
                if (tiles.contains(neighbour.getComponentName())) continue;
                addNeighbours(neighbour, tiles);
            }
        }
        return islands;
    }

    static void addNeighbours(BoardNode n, List<String> tiles) {
        if (tiles.contains(n.getComponentName())) return;
        tiles.add(n.getComponentName());
        for (BoardNode neighbour : n.getNeighbours().keySet()) {
            if (tiles.contains(neighbour.getComponentName())) continue;
            addNeighbours(neighbour, tiles);
        }
    }

    static boolean legalSpawns(Quest quest) {
        String heroTile = quest.getStartingTile();
        if (illegalHeroSpawns.contains(heroTile)) return false;

        List<String> occupied = new ArrayList<>();
        occupied.add(heroTile);

        for (String[] monster : quest.getMonsters())
        {
            String monsterTile = monster[1];
            if (occupied.contains(monsterTile)) return false;
            occupied.add(monsterTile);
            // Lieutenants can be placed anywhere that Heroes can
            String monsterName = monster[0];
            if (monsterName.contains("lieutenant")) continue;
            if (illegalMonsterSpawns.contains(monsterTile)) return false;
            if (monsterName.contains("Barghest")) {
                if (illegalBarghestSpawns.contains(monsterTile)) return false;
            }
            if (monsterName.contains("Dragon")) {
                if (illegalBarghestSpawns.contains(monsterTile)) return false;
                if (illegalDragonSpawns.contains(monsterTile)) return false;
            }
        }
        return true;
    }

    static int getBoardSize(GraphBoard board) {
        int size = 0;
        List<String> nodes = new ArrayList<>();
        for (BoardNode node : board.getBoardNodes()) {
            nodes.add(node.getComponentName().split("-")[0]);
        }
        int checked = 0;
        for (String node : nodes) {
            for (GridBoard tile : GenerateBoards.tiles) {
                if (tile.getComponentName().contains(node)) {
                    checked++;
                    size += Integer.parseInt(tile.getProperty(900).toString());
                    break;
                }
            }
        }
        return size;
    }

    static boolean noRepeats(Quest quest) {
        List<String> monsters = new ArrayList<>();
        for (String[] monster : quest.getMonsters()) {
            String name = monster[0].split(":")[0];
            if (name.contains("Open")) continue;
            if (monsters.contains(name)) return true;
            monsters.add(name);
        }
        return false;
    }

    static float consistency (GraphBoard board) {
        int errors = 0;
        int connections = 0;

        BoardNode[] nodes = board.getBoardNodeMap().values().toArray(new BoardNode[0]);
        for (BoardNode node : nodes) {
            String tile = node.getComponentName();
            boolean checkTransition = tile.contains("transition");
            if (!checkTransition) {
                for (BoardNode n : node.getNeighbours().keySet()) {
                    connections++;
                    String neighbour = n.getComponentName();
                    if ((tile.contains("A") && tile.contains("B")) || (tile.contains("B") && neighbour.contains("A")))
                        errors++;
                }
            }
            else {
                int orientation = ((PropertyInt) node.getProperty("orientation")).value;
                String[] cons = ((PropertyStringArray) node.getProperty("connections")).getValues();
                String[] neighbours = ((PropertyStringArray) node.getProperty("neighbours")).getValues();
                for (int i = 0; i < neighbours.length; i++) {
                    connections++;
                    String neighbour = neighbours[i];
                    String connect = cons[i];
                    switch (orientation) {
                        case 0:
                            if (neighbour.contains("A") && !connect.contains("N"))
                                errors++;
                            if (neighbour.contains("B") && !connect.contains("S"))
                                errors++;
                            break;
                        case 1:
                            if (neighbour.contains("A") && !connect.contains("E"))
                                errors++;
                            if (neighbour.contains("B") && !connect.contains("W"))
                                errors++;
                            break;
                        case 2:
                            if (neighbour.contains("A") && !connect.contains("S"))
                                errors++;
                            if (neighbour.contains("B") && !connect.contains("N"))
                                errors++;
                            break;
                        case 3:
                            if (neighbour.contains("A") && !connect.contains("W"))
                                errors++;
                            if (neighbour.contains("B") && !connect.contains("E"))
                                errors++;
                            break;
                    }
                }
            }
        }
        return (float) (connections - errors) / connections;
    }

    static float getMonsterHealth(Quest quest) {
        float monsterHealth = 0f;
        float totalMonsters = 0f;

        int act = quest.getAct();

        for (String[] monster : quest.getMonsters()) {
            String[] name = monster[0].split(":");
            if (name[0].contains("Open")) {
                // Totals for OpenSmall: 4.6 Monsters
                // Act 1 - 16.4 HP
                // Act 2 - 25.2 HP
                if (name[0].contains("Small")) {
                    totalMonsters += 4.6f;
                    monsterHealth += (act == 1) ? 16.4f : 25.2f;
                }
                // Totals for Open: 3.4444 Monsters
                // Act 1 - 14.6666 HP
                // Act 2 - 21.5555 HP
                else {
                    totalMonsters += (float) 31 / 9;
                    monsterHealth += (act == 1) ? (float) 132 / 9 : (float) 194 / 9;
                }
            }
            else if (name[1].contains("lieutenant")) {
                totalMonsters++;
                monsterHealth += GenerateBoards.lieutenants.get(name[0]).get(act + "-4").getAttributeValue(Figure.Attribute.Health);
            }
            else {
                HashMap<String, Monster> mon = GenerateBoards.monsters.get(name[0]);
                int[] setup = ((PropertyIntArray) mon.get("super").getProperty("setup")).getValues();
                int minionCount = setup[setup.length - 1];
                totalMonsters += 1 + minionCount;
                monsterHealth += mon.get(act + "-master").getAttributeValue(Figure.Attribute.Health);
                monsterHealth += minionCount * mon.get(act + "-minion").getAttributeValue(Figure.Attribute.Health);
            }
        }


        if (totalMonsters > 0)
            monsterHealth = monsterHealth / totalMonsters;
        return monsterHealth;
    }

    static List<Float> getFitness(Quest quest, GraphBoard board) {
        List<Float> scores = new ArrayList<>();

        // Connectedness
        float connected = connectedness(board);

        // Map Size
        float size = getBoardSize(board);

        // Geometry
        Pair<int[][], Integer> result = createBoard(quest, board);
        float geometry = size == (float) result.b ? 1f : 0f;

        // Monster Group Repeats
        // Inverse Boolean = Score 1 if no repeats, 0 if repeats found
        float repeats = noRepeats(quest) ? 0f : 1f;

        // Legal Spawning
        // Boolean = Score 1 if all legal, 0 if conflict
        float spawning = legalSpawns(quest) ? 1f : 0f;

        // Map Consistency
        float consistency = consistency(board);

        // Monster Groups
        float groups = quest.getMonsters().size();

        // Monster Health
        float health = getMonsterHealth(quest);

        // Map Complexity
        float complexity = 1f;

        // Map Rules
        float rules = 1f;

        scores.add(connected);
        scores.add(geometry);
        scores.add(repeats);
        scores.add(spawning);
        scores.add(consistency);
        scores.add(size);
        scores.add(groups);
        scores.add(health);
        scores.add(complexity);
        scores.add(rules);

        float fitness = fitness(scores);
        scores.add(fitness);

        boolean feasible = GenerateBoards.checkFeasible(scores);
        System.out.println(feasible);

        return scores;
    }

    static Pair<int[][], Integer> createBoard(Quest q, GraphBoard b) {

        // 3. Put together the master grid board
        // Find maximum board width and height, if all were put together side by side
        int width = 0;
        int height = 0;
        for (BoardNode bn : b.getBoardNodes()) {
            // Find width of this tile, according to orientation
            GridBoard tile = GenerateBoards.getTileByName(bn.getComponentName());
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
        BoardNode[][] board = new BoardNode[height][width];  // Board nodes here will be the individual cells in the tiles
        int[][] tileReferences = new int[height][width];  // Reference to component ID of tile placed at that position
        HashMap<BoardNode, BoardNode> drawn = new HashMap<>();  // Keeps track of which tiles have been added to the board already, for recursive purposes

        // StartX / Y Will need to be adjusted to not draw on top of existing things

        // Find first tile, as board node in the board configuration graph board
        int x = 0;
        List<Object> nodes = Arrays.asList(b.getBoardNodeMap().values().toArray());
        BoardNode firstTile = (BoardNode) nodes.get(x);
        String startingTileName = q.getStartingTile();
        while (!firstTile.getComponentName().contains(startingTileName) && x < nodes.size() - 1) {
            x++;
            firstTile = (BoardNode) nodes.get(x);
        }
        // System.out.println("First tile:" + firstTile.getComponentName());
        if (firstTile != null) {
            // Find grid board of first tile, rotate to correct orientation and add its tiles to the board
            GridBoard tile = GenerateBoards.getTileByName(firstTile.getComponentName());
            int orientation = ((PropertyInt) firstTile.getProperty(orientationHash)).value;
            Component[][] rotated = tile.rotate(orientation);
            int startX = width / 2 - rotated[0].length / 2;
            int startY = height / 2 - rotated.length / 2;
            // Bounds will keep track of where tiles actually exist in the master board, to trim to size later
            Rectangle bounds = new Rectangle(startX, startY, rotated[0].length, rotated.length);
            // Recursive call, will add all tiles in relation to their neighbours as per the board configuration
            addTilesToBoard(null, firstTile, startX, startY, board, null, GenerateBoards.tiles, tileReferences, drawn, bounds, null);

            BoardNode[][] trimBoard = new BoardNode[bounds.height][bounds.width];
            int[][] trimTileRef = new int[bounds.height][bounds.width];
            for (int i = 0; i < bounds.height; i++) {
                System.arraycopy(board[i + bounds.y], bounds.x, trimBoard[i], 0, bounds.width);
                System.arraycopy(tileReferences[i + bounds.y], bounds.x, trimTileRef[i], 0, bounds.width);
            }
            int sizeCounter = 0;
            for (int i = 0; i < trimTileRef.length; i++) {
                for (int j = 0; j < trimTileRef[i].length; j++) {
                    if (trimTileRef[i][j] != 0)
                        sizeCounter++;
                }
            }

            return new Pair<>(trimTileRef, sizeCounter);
        }
        return null;
    }

    private static void addTilesToBoard(BoardNode parentTile, BoardNode tileToAdd, int x, int y, BoardNode[][] board,
                                 BoardNode[][] tileGrid,
                                 List<GridBoard> tiles,
                                 int[][] tileReferences,
                                 Map<BoardNode, BoardNode> drawn,
                                 Rectangle bounds,
                                 String sideWithOpening) {
        if (!drawn.containsKey(parentTile) || !drawn.get(parentTile).equals(tileToAdd)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = GenerateBoards.getTileByName(tileToAdd.getComponentName());
            BoardNode[][] originalTileGrid = tile.rotate(((PropertyInt) tileToAdd.getProperty(orientationHash)).value);
            if (tileGrid == null) {
                tileGrid = originalTileGrid;
            }
            int height = tileGrid.length;
            int width = tileGrid[0].length;

            // Add cells from new tile to the master board
            for (int i = y; i < y + height; i++) {
                for (int j = x; j < x + width; j++) {
                    // Avoid removing already set tiles
                    if (tileGrid[i - y][j - x] == null) continue;
                    if (tileGrid[i - y][j - x].getComponentName().equalsIgnoreCase("null")) continue;
                    if (board[i][j] != null && !board[i][j].getComponentName().equalsIgnoreCase("null")) continue;
                    if (!DescentTypes.TerrainType.isInsideTerrain(tileGrid[i - y][j - x].getComponentName())) continue;

                    // Set
                    board[i][j] = tileGrid[i - y][j - x].copy();
                    board[i][j].setProperty(new PropertyInt("connections", (tileToAdd.getComponentID()+1)));

                    // Don't keep references for edge tiles
                    if (board[i][j] == null || board[i][j].getComponentName().equals("edge")
                            || board[i][j].getComponentName().equals("open")) continue;

                    // Set references
                    tileReferences[i][j] = (tile.getComponentID()+1);
                    }
            }

            // Add connections at opening side with existing tiles
            addConnectionsAtOpeningOnSide(board, originalTileGrid, x, y, width, height, sideWithOpening);

            // Add connections inside the tile, ignoring blocked spaces
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    BoardNode currentSpace = tileGrid[i][j];
                    if (currentSpace != null && (DescentTypes.TerrainType.isWalkableTerrain(currentSpace.getComponentName())
                            || currentSpace.getComponentName().equalsIgnoreCase("pit"))) {  // pits connect to walkable spaces only (pit-pit not allowed)
                        List<Vector2D> boardNs = getNeighbourhood(j, i, width, height, true);
                        for (Vector2D n2 : boardNs) {
                            if (tileGrid[n2.getY()][n2.getX()] != null && DescentTypes.TerrainType.isWalkableTerrain(tileGrid[n2.getY()][n2.getX()].getComponentName())) {
                                board[n2.getY() + y][n2.getX() + x].addNeighbourWithCost(board[i + y][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                                board[i + y][j + x].addNeighbourWithCost(board[n2.getY() + y][n2.getX() + x], DescentTypes.TerrainType.getMovePointsCost(board[n2.getY() + y][n2.getX() + x].getComponentName()));
                            }
                        }
                    }
                }
            }

            // This tile was drawn
//            drawn.add(tileToAdd);
            drawn.put(parentTile, tileToAdd);

            // Draw neighbours
            for (BoardNode neighbour : tileToAdd.getNeighbours().keySet()) {

                if (drawn.containsKey(neighbour)) continue;

                // Find location to start drawing neighbour
                Pair<String, Vector2D> connectionToNeighbour = findConnection(tileToAdd, neighbour, findOpenings(tileGrid));

                if (connectionToNeighbour != null) {
                    connectionToNeighbour.b.add(x, y);
                    // Find orientation and opening connection from neighbour, generate top-left corner of neighbour from that
                    GridBoard tileN = GenerateBoards.getTileByName(neighbour.getComponentName());
                    if (tileN != null) {
                        BoardNode[][] tileGridN = tileN.rotate(((PropertyInt) neighbour.getProperty(orientationHash)).value);

                        // Find location to start drawing neighbour
                        Pair<String, Vector2D> conn2 = findConnection(neighbour, tileToAdd, findOpenings(tileGridN));

                        int w = tileGridN[0].length;
                        int h = tileGridN.length;

                        if (conn2 != null) {
                            String side = conn2.a;
                            Vector2D connectionFromNeighbour = conn2.b;
                            if (side.equalsIgnoreCase("W")) {
                                // Remove first column
                                BoardNode[][] tileGridNTrim = new BoardNode[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 1, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("E")) {
                                connectionFromNeighbour.subtract(1, 0);
                                // Remove last column
                                BoardNode[][] tileGridNTrim = new BoardNode[h][w - 1];
                                for (int i = 0; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w - 1);
                                }
                                tileGridN = tileGridNTrim;
                            } else if (side.equalsIgnoreCase("N")) {
                                // Remove first row
                                BoardNode[][] tileGridNTrim = new BoardNode[h - 1][w];
                                for (int i = 1; i < h; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i - 1], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            } else {
                                connectionFromNeighbour.subtract(0, 1);
                                // Remove last row
                                BoardNode[][] tileGridNTrim = new BoardNode[h - 1][w];
                                for (int i = 0; i < h - 1; i++) {
                                    System.arraycopy(tileGridN[i], 0, tileGridNTrim[i], 0, w);
                                }
                                tileGridN = tileGridNTrim;
                            }

                            // Update area bounds
                            Vector2D topLeftCorner = new Vector2D(connectionToNeighbour.b.getX() - connectionFromNeighbour.getX(),
                                    connectionToNeighbour.b.getY() - connectionFromNeighbour.getY());
                            Vector2D bottomRightCorner = new Vector2D(topLeftCorner.getX() + tileGridN[0].length,
                                    topLeftCorner.getY() + tileGridN.length);
                            Vector2D oldTopLeft = new Vector2D(bounds.x, bounds.y);
                            Vector2D oldBottomRight = new Vector2D((int) bounds.getMaxX(), (int) bounds.getMaxY());

                            int deltaMinX = oldTopLeft.getX() - topLeftCorner.getX();
                            if (deltaMinX > 0) {
                                bounds.x = topLeftCorner.getX();
                                bounds.width += deltaMinX;
                            }
                            int deltaMinY = oldTopLeft.getY() - topLeftCorner.getY();
                            if (deltaMinY > 0) {
                                bounds.y = topLeftCorner.getY();
                                bounds.height += deltaMinY;
                            }
                            int deltaMaxX = bottomRightCorner.getX() - oldBottomRight.getX();
                            if (deltaMaxX > 0)
                                bounds.width += deltaMaxX;
                            int deltaMaxY = bottomRightCorner.getY() - oldBottomRight.getY();
                            if (deltaMaxY > 0)
                                bounds.height += deltaMaxY;

                            // Draw neighbour recursively
                            addTilesToBoard(tileToAdd, neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tileGridN,
                                    tiles, tileReferences, drawn, bounds, side);
                        }
                    }
                }
            }
        }
    }

    private static Pair<String, Vector2D> findConnection(BoardNode from, BoardNode to, HashMap<String, ArrayList<Vector2D>> openings) {
        String[] neighbours = ((PropertyStringArray) from.getProperty(neighbourHash)).getValues();
        String[] connections = ((PropertyStringArray) from.getProperty(connectionHash)).getValues();

        for (int i = 0; i < neighbours.length; i++) {
            if (neighbours[i].equalsIgnoreCase(to.getComponentName())) {
                String conn = connections[i];

                String side = conn.split("-")[0];
                int countFromTop = Integer.parseInt(conn.split("-")[1]);
                if (openings.containsKey(side)) {
                    if (countFromTop >= 0 && countFromTop < openings.get(side).size()) {
                        return new Pair<>(side, openings.get(side).get(countFromTop));
                    }
                }
                break;
            }
        }
        return null;
    }

    private static HashMap<String, ArrayList<Vector2D>> findOpenings(BoardNode[][] tileGrid) {
        int height = tileGrid.length;
        int width = tileGrid[0].length;

        HashMap<String, ArrayList<Vector2D>> openings = new HashMap<>();
        // TOP, check each column, stop at the first encounter in each column.
        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile above
                    if (i == 0 || tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || tileGrid[i][j - 1] != null && j < width - 1 && tileGrid[i][j + 1] != null
                                && !tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("open"))
                                && (j < width - 1 && tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("open"))) {
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
            for (int i = height - 1; i >= 0; i--) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile below
                    if (i == height - 1 || tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" to the left (already included, all openings 2-wide)
                        // But another "open" to the right
                        if ((j == 0 || !tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("open")) &&
                                (j < width - 1 && tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("open"))) {
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
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the left
                    if (j == 0 || tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i][j - 1].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("open")) &&
                                (i < height - 1 && tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("open"))) {
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
        for (int i = 0; i < height; i++) {
            for (int j = width - 1; j >= 0; j--) {
                if (tileGrid[i][j] != null && tileGrid[i][j].getComponentName().equalsIgnoreCase("open")) {
                    // Check valid: nothing, null, or edge tile to the right
                    if (j == width - 1 || tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("null") ||
                            tileGrid[i][j + 1].getComponentName().equalsIgnoreCase("edge")) {
                        // Check valid: nothing or not "open" above (already included, all openings 2-wide)
                        // But another "open" below
                        if ((i == 0 || !tileGrid[i - 1][j].getComponentName().equalsIgnoreCase("open")) &&
                                (i < height - 1 && tileGrid[i + 1][j].getComponentName().equalsIgnoreCase("open"))) {
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

    private static void addConnectionsAtOpeningOnSide(BoardNode[][] board, BoardNode[][] originalTileGrid,
                                               int x, int y, int width, int height, String side) {
        if (side != null) {
            if (side.equalsIgnoreCase("n")) {
                // Nodes at opening that should connect are on the top row. Above them on original tile grid there is an "open" space
                int i = 0;
                for (int j = 0; j < width; j++) {
                    if (originalTileGrid[i][j] != null &&  // Same row, in the tile that was placed this is trimmed
                            originalTileGrid[i][j].getComponentName().equalsIgnoreCase("open")
                            && board[i + y - 1][j + x] != null) {
                        // Add connections for this node
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y][j + x] != null && board[i + y - 1][j + x1] != null) {
                                    board[i + y][j + x].addNeighbourWithCost(board[i + y - 1][j + x1], DescentTypes.TerrainType.getMovePointsCost(board[i + y - 1][j + x1].getComponentName()));
                                    board[i + y - 1][j + x1].addNeighbourWithCost(board[i + y][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                                }
                            }
                        }
                        // And connections back from the node in front too
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y - 1][j + x] != null && board[i + y][j + x1] != null) {
                                    board[i + y - 1][j + x].addNeighbourWithCost(board[i + y][j + x1], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x1].getComponentName()));
                                    board[i + y][j + x1].addNeighbourWithCost(board[i + y - 1][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y - 1][j + x].getComponentName()));
                                }
                            }
                        }
                    }
                }
            } else if (side.equalsIgnoreCase("s")) {
                // Nodes at opening that should connect are on the bottom row. Below them is an "open" space
                int i = height - 1;
                for (int j = 0; j < width; j++) {
                    if (originalTileGrid[i + 1][j] != null &&  // Next row
                            originalTileGrid[i + 1][j].getComponentName().equalsIgnoreCase("open")
                            && board[i + y + 1][j + x] != null) {
                        // Add connections for this node
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y][j + x] != null && board[i + y + 1][j + x1] != null) {
                                    board[i + y][j + x].addNeighbourWithCost(board[i + y + 1][j + x1], DescentTypes.TerrainType.getMovePointsCost(board[i + y + 1][j + x1].getComponentName()));
                                    board[i + y + 1][j + x1].addNeighbourWithCost(board[i + y][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                                }
                            }
                        }
                        // And connections back from the node in front too
                        for (int x1 = x - 1; x1 <= x + 1; x1++) {
                            if (j + x1 >= 0 && j + x1 < board[0].length) {
                                if (board[i + y + 1][j + x] != null && board[i + y][j + x1] != null) {
                                    board[i + y + 1][j + x].addNeighbourWithCost(board[i + y][j + x1], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x1].getComponentName()));
                                    board[i + y][j + x1].addNeighbourWithCost(board[i + y + 1][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y + 1][j + x].getComponentName()));
                                }
                            }
                        }
                    }
                }
            } else if (side.equalsIgnoreCase("e")) {
                // Nodes are on the rightmost column. To their right is "open"
                int j = width - 1;
                for (int i = 0; i < height; i++) {
                    if (originalTileGrid[i][j + 1] != null &&  // Next column
                            originalTileGrid[i][j + 1].getComponentName().equalsIgnoreCase("open")
                            && board[i + y][j + x + 1] != null) {
                        // Add connections for this node
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x] != null && board[i + y1][j + x + 1] != null) {
                                board[i + y][j + x].addNeighbourWithCost(board[i + y1][j + x + 1], DescentTypes.TerrainType.getMovePointsCost(board[i + y1][j + x + 1].getComponentName()));
                                board[i + y1][j + x + 1].addNeighbourWithCost(board[i + y][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                            }
                        }
                        // And connections back from the node in front too
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x + 1] != null && board[i + y1][j + x] != null) {
                                board[i + y][j + x + 1].addNeighbourWithCost(board[i + y1][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y1][j + x].getComponentName()));
                                board[i + y1][j + x].addNeighbourWithCost(board[i + y][j + x + 1], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x + 1].getComponentName()));
                            }
                        }
                    }
                }
            } else if (side.equalsIgnoreCase("w")) {
                // Nodes are in the first column (leftmost). To their left is "open"
                int j = 0;
                for (int i = 0; i < height; i++) {
                    if (originalTileGrid[i][j] != null &&  // Same column, trimmed in tile that was placed
                            originalTileGrid[i][j].getComponentName().equalsIgnoreCase("open")
                            && board[i + y][j + x - 1] != null) {
                        // Add connections for this node
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x] != null && board[i + y1][j + x - 1] != null) {
                                board[i + y][j + x].addNeighbourWithCost(board[i + y1][j + x - 1], DescentTypes.TerrainType.getMovePointsCost(board[i + y1][j + x - 1].getComponentName()));
                                board[i + y1][j + x - 1].addNeighbourWithCost(board[i + y][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x].getComponentName()));
                            }
                        }
                        // And connections back from the node in front too
                        for (int y1 = y - 1; y1 <= y + 1; y1++) {
                            if (i + y1 >= 0 && i + y1 < board.length
                                    && board[i + y][j + x - 1] != null && board[i + y1][j + x] != null) {
                                board[i + y][j + x - 1].addNeighbourWithCost(board[i + y1][j + x], DescentTypes.TerrainType.getMovePointsCost(board[i + y1][j + x].getComponentName()));
                                board[i + y1][j + x].addNeighbourWithCost(board[i + y][j + x - 1], DescentTypes.TerrainType.getMovePointsCost(board[i + y][j + x - 1].getComponentName()));
                            }
                        }
                    }
                }
            }
        }
    }

}
