package games.descent2e.pcg;

import core.components.BoardNode;
import core.components.Component;
import core.components.GraphBoard;
import core.components.GridBoard;
import core.properties.PropertyInt;
import core.properties.PropertyIntArray;
import core.properties.PropertyString;
import core.properties.PropertyStringArray;
import games.descent2e.DescentTypes;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;
import games.descent2e.concepts.Quest;
import utilities.Pair;
import utilities.Vector2D;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

import static core.CoreConstants.*;
import static games.descent2e.DescentConstants.connectionHash;
import static games.descent2e.pcg.ControlVariables.*;
import static utilities.Utils.getNeighbourhood;

public class FitnessFunction {
    // Default - requirements for feasible
    public float W_CONNECTED = 1;
    public float W_GEOMETRY = 1;
    public float W_REPEATS = 1;
    public float W_SPAWNING = 1;
    public float W_CONSISTENCY = 1;

    // What we're actually measuring for the user's requested board
    public float W_SIZE = 1;
    public float W_GROUP = 1;
    public float W_HEALTH = 1;
    public float W_HEIGHT = 0;
    public float W_WIDTH = 0;

    // Not yet implemented/relevant
    public float W_COMPLEXITY = 0;
    public float W_RULES = 0;

    public float W_TOTAL = W_CONNECTED + W_GEOMETRY + W_REPEATS + W_SPAWNING + W_CONSISTENCY +
                            W_SIZE + W_GROUP + W_HEALTH + W_HEALTH + W_HEIGHT + W_WIDTH + W_COMPLEXITY + W_RULES;

    public int IDEAL_SIZE = 166;
    public int IDEAL_GROUP = 5;
    public float IDEAL_HEALTH = 5.872f;
    public int IDEAL_HEIGHT = 18;
    public int IDEAL_WIDTH = 15;
    public int IDEAL_COMPLEXITY = 1;
    public int IDEAL_RULES = 1;

    CreateOffspring co;

    public FitnessFunction(CreateOffspring co, int size, int group, float health, int height, int width) {
        this.co = co;
        IDEAL_SIZE = size;
        IDEAL_GROUP = group;
        IDEAL_HEALTH = health;
        IDEAL_HEIGHT = height;
        IDEAL_WIDTH  = width;
    }

    public FitnessFunction(CreateOffspring co, int size, int group, float health, int height, int width, int complexity, int rules) {
        this.co = co;
        IDEAL_SIZE = size;
        IDEAL_GROUP = group;
        IDEAL_HEALTH = health;
        IDEAL_HEIGHT = height;
        IDEAL_WIDTH  = width;
        IDEAL_COMPLEXITY = complexity;
        IDEAL_RULES = rules;
    }

    void setWeights(float connected, float geometry, float repeats, float spawning, float consistency, float size, float group, float health, float height, float width, float complexity, float rules) {
        W_CONNECTED = connected;
        W_GEOMETRY = geometry;
        W_REPEATS = repeats;
        W_SPAWNING = spawning;
        W_CONSISTENCY = consistency;
        W_SIZE = size;
        W_GROUP = group;
        W_HEALTH = health;
        W_HEIGHT = height;
        W_WIDTH = width;
        W_COMPLEXITY = complexity;
        W_RULES = rules;
        updateTotalWeights();
    }

    void setWeights (float size, float group, float health, float height, float width){
        setWeights(1,1,1,1,1, size, group, health, height, width, 0, 0);
    }

    private void updateTotalWeights() {
        W_TOTAL = W_CONNECTED + W_GEOMETRY + W_REPEATS + W_SPAWNING + W_CONSISTENCY
                + W_SIZE + W_GROUP + W_HEALTH + W_COMPLEXITY + W_RULES + W_HEALTH + W_WIDTH;
    }

    private float fitness(HashMap<String, Float> scores) {
        updateTotalWeights();

        float fitness = 0f;

        fitness += W_CONNECTED * (1 / scores.get("Connectedness"));
        fitness += W_GEOMETRY * scores.get("Geometry");
        fitness += W_REPEATS * scores.get("Repeats");
        fitness += W_SPAWNING * scores.get("Spawning");
        fitness += W_CONSISTENCY * scores.get("Consistency");

        fitness += W_SIZE * (1f - (Math.abs(IDEAL_SIZE - scores.get("Size")) / IDEAL_SIZE));
        fitness += W_GROUP * (1f - (Math.abs(IDEAL_GROUP - scores.get("Groups")) / IDEAL_GROUP));
        fitness += W_HEALTH * (1f - (Math.abs(IDEAL_HEALTH - scores.get("Health")) / IDEAL_HEALTH));
        fitness += W_HEIGHT * (1f - (Math.abs(IDEAL_HEIGHT - scores.get("Height")) / IDEAL_HEIGHT));
        fitness += W_WIDTH * (1f - (Math.abs(IDEAL_WIDTH - scores.get("Width")) / IDEAL_WIDTH));

        fitness += W_COMPLEXITY * (1f - (Math.abs(IDEAL_COMPLEXITY - scores.get("Complexity")) / IDEAL_COMPLEXITY));
        fitness += W_RULES * (1f - (Math.abs(IDEAL_RULES - scores.get("Rules")) / IDEAL_RULES));

        // Round it to out to 10
        fitness = fitness * 10f / W_TOTAL;

        return fitness;
    }

    private int connectedness(PCGBoard quest) {
        int islands = 0;

        HashSet<String> tiles = new HashSet<>();
        for (PCGNode node : quest.board) {
            if (!tiles.contains(node.name))
                islands++;
            else
                tiles.add(node.name);
            for (Connection c : node.neighbours.keySet()) {
                String next = node.neighbours.get(c);
                if (tiles.contains(next)) continue;
                for (PCGNode n : quest.board)
                    if (n.name.equals(next)) {
                        addNeighbours(quest, n, tiles);
                        break;
                    }
            }
        }
        return islands;
    }

    private int freeEdges(List<PCGNode> board) {
        int freeEdges = 0;

        for (PCGNode node : board) {
            for (Connection c : node.neighbours.keySet()) {
                if (node.neighbours.get(c).equals("null"))
                    freeEdges++;
            }
            int expected = node.maxConnections;
            if (node.neighbours.size() != expected)
                freeEdges += Math.max(expected - node.neighbours.size(), 0);
        }

        return freeEdges;
    }

    private void addNeighbours(PCGBoard quest, PCGNode node, HashSet<String> tiles) {
        if (tiles.contains(node.name)) return;
        tiles.add(node.name);
        for (Connection c : node.neighbours.keySet()) {
            String next = node.neighbours.get(c);
            if (tiles.contains(next)) continue;
            for (PCGNode n : quest.board)
                if (n.name.equals(next)) {
                    addNeighbours(quest, n, tiles);
                    break;
                }
        }
    }

    private boolean legalSpawns(CreateOffspring co, PCGBoard quest) {
        String heroTile = quest.heroStartingPosition;
        if (heroTile == null)
            return false;
        if (illegalHeroSpawns.contains(heroTile.split("-")[0]))
            return false;

        HashSet<String> allNodes = new HashSet<>();
        for (PCGNode node : quest.board)
            allNodes.add(node.name);

        HashSet<String> occupied = new HashSet<>();
        HashMap<String, Integer> occupiedSize = new HashMap<>();

        occupied.add(heroTile);
        GridBoard node = co.getTileByName(heroTile);
        assert node != null;
        // Subtract 4 from the available space, one for each Hero
        occupiedSize.put(heroTile, ((PropertyInt) node.getProperty(spaceHash)).value - 4);

        HashSet<String> traits = quest.monsterTraits;
        boolean barghestOpen = traits.contains("Dark") || traits.contains("Wilderness") || traits.contains("All");
        boolean dragonOpen = traits.contains("Dark") || traits.contains("Cave") || traits.contains("All");

        //String result = "Heroes: " + heroTile;

        for (String[] monster : quest.monsters)
        {
            String monsterName = monster[0];
            String monsterTile = monster[1];

            //result += "; " + monsterName + ": " + monsterTile;

            // Make sure the tile is actually valid in the first place
            if (monsterTile == null || !allNodes.contains(monsterTile))
                return false;

            // Lieutenants can be placed anywhere that Heroes can
            boolean dragon = monsterName.contains("Open") && !monsterName.contains("OpenSmall") && dragonOpen;
            boolean barghest = monsterName.contains("Open") && barghestOpen;

            if (!monsterName.contains("lieutenant")) {
                if(illegalMonsterSpawns.contains(monsterTile.split("-")[0]))
                    return false;
                if (monsterName.contains("Barghest") || barghest) {
                    if(illegalBarghestSpawns.contains(monsterTile.split("-")[0]))
                        return false;
                }
                if (monsterName.contains("Dragon") || dragon) {
                    if(illegalBarghestSpawns.contains(monsterTile.split("-")[0]))
                        return false;
                    if(illegalDragonSpawns.contains(monsterTile.split("-")[0]))
                        return false;
                }
            }

            if (occupied.contains(monsterTile)) {
                int size = occupiedSize.get(monsterTile);
                if (monsterName.contains("lieutenant")) {
                    size -= 1;
                }
                else {
                    Monster mon;
                    if (dragon)
                        mon = GenerateBoards.monsters.get("Shadow Dragon").get("super");
                    else if (barghest)
                        mon = GenerateBoards.monsters.get("Barghest").get("super");
                    else if (monsterName.contains("OpenSmall"))
                        mon = GenerateBoards.monsters.get("Goblin Archer").get("super");
                    else if (monsterName.contains("Open"))
                        mon = GenerateBoards.monsters.get("Ettin").get("super");
                    else
                        mon = GenerateBoards.monsters.get(monsterName.split(":")[0]).get("super");
                    int count = ((PropertyIntArray) mon.getProperty("setup")).getValues()[2] + 1;
                    String[] mSize = ((PropertyString) mon.getProperty("size")).value.split("x");
                    int space = Integer.parseInt(mSize[0]) * Integer.parseInt(mSize[1]) * count;
                    size -= space;
                }
                if (size < 0)
                    return false;
                occupiedSize.put(monsterTile, size);
            }
            else {
                occupied.add(monsterTile);
                for (PCGNode n : quest.board) {
                    if (n.name.equals(monsterTile)) {
                        occupiedSize.put(monsterTile, n.size);
                        break;
                    }
                }
            }
        }

        //System.out.println(result);

        return true;
    }

    private int getBoardSize(List<PCGNode> board) {
        int size = 0;
        for (PCGNode node : board) {
            size += node.size;
        }
        return size;
    }

    private boolean noRepeats(PCGBoard quest) {
        List<String> monsters = new ArrayList<>();
        for (String[] monster : quest.monsters) {
            String name = monster[0].split(":")[0];
            if (name.contains("Open")) continue;
            if (monsters.contains(name)) return true;
            monsters.add(name);
        }
        return false;
    }

    private float consistency (List<PCGNode> board) {
        int errors = 0;
        int connections = 0;

        for (PCGNode node : board) {
            String tile = node.name;
            boolean checkTransition = tile.contains("transition");
            if (!checkTransition) {
                for (Connection c : node.neighbours.keySet()) {
                    String neighbour = node.neighbours.get(c);
                    connections++;
                    if ((tile.contains("A") && neighbour.contains("B")) || (tile.contains("B") && neighbour.contains("A")))
                        errors++;
                }
            }
            else {
                int orientation = node.orientation;
                for (Connection c : node.neighbours.keySet()) {
                    String neighbour = node.neighbours.get(c);
                    connections++;
                    switch (orientation) {
                        case 0:
                            if (neighbour.contains("A") && !c.equals(Connection.NORTH))
                                errors++;
                            if (neighbour.contains("B") && !c.equals(Connection.SOUTH))
                                errors++;
                            break;
                        case 1:
                            if (neighbour.contains("A") && !c.equals(Connection.EAST))
                                errors++;
                            if (neighbour.contains("B") && !c.equals(Connection.WEST))
                                errors++;
                            break;
                        case 2:
                            if (neighbour.contains("A") && !c.equals(Connection.SOUTH))
                                errors++;
                            if (neighbour.contains("B") && !c.equals(Connection.NORTH))
                                errors++;
                            break;
                        case 3:
                            if (neighbour.contains("A") && !c.equals(Connection.WEST))
                                errors++;
                            if (neighbour.contains("B") && !c.equals(Connection.EAST))
                                errors++;
                            break;
                    }
                }
            }
        }
        return (float) (connections - errors) / connections;
    }

    private Pair<Float, Float> getMonsterHealth(PCGBoard quest) {
        float monsterHealth = 0f;
        float totalMonsters = 0f;

        int act = quest.act;

        for (String[] monster : quest.monsters) {
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

        return new Pair<>(monsterHealth, totalMonsters);
    }

    HashMap<String, Float> getFitness(CreateOffspring co, PCGBoard quest) throws InterruptedException, InvocationTargetException {
        HashMap<String, Float> scores = new HashMap<>();

        List<PCGNode> board = quest.board;

        // Connectedness
        float connected = connectedness(quest);

        float freeEdge = freeEdges(board);

        // Map Size
        float size = getBoardSize(board);

        float tiles = (float) board.size();

        // Geometry
        Pair<int[][], Integer> result = createBoard(co, quest);
        float geometry = 0f;
        float height = 0f;
        float width = 0f;
        if (result != null) {
            geometry = size == (float) result.b ? 1f : 0f;

            // Subtract 2 from the array size of height/width, as there is a 1 tile buffer perimeter
            // Remove the top/bottom and left/right buffer in our calculations
            height = result.a.length - 2;
            width = result.a[0].length - 2;
        }
        // Monster Group Repeats
        // Inverse Boolean = Score 1 if no repeats, 0 if repeats found
        float repeats = noRepeats(quest) ? 0f : 1f;

        // Legal Spawning
        // Boolean = Score 1 if all legal, 0 if conflict
        float spawning = legalSpawns(co, quest) ? 1f : 0f;

        // Map Consistency
        float consistency = consistency(board);

        // Monster Groups
        float groups = quest.monsters.size();

        // Monster Health
        Pair<Float, Float> health = getMonsterHealth(quest);

        float averageHealth = (health.b > 0) ? health.a / health.b : health.a;

        // Map Complexity
        float complexity = 1f;

        // Map Rules
        float rules = 1f;

        scores.put("ID", (float) co.nowServing);
        scores.put("Connectedness", connected);
        scores.put("Free Edges", freeEdge);
        scores.put("Geometry", geometry);
        scores.put("Repeats", repeats);
        scores.put("Spawning", spawning);
        scores.put("Consistency", consistency);
        scores.put("Size", size);
        scores.put("Physical Size", Float.valueOf(result.b));
        scores.put("Height", height);
        scores.put("Width", width);
        scores.put("Tile Count", tiles);
        scores.put("Groups", groups);
        scores.put("Monster Count", health.b);
        scores.put("Health", averageHealth);
        scores.put("Total Health", health.a);
        scores.put("Complexity", complexity);
        scores.put("Rules", rules);
        scores.put("Act", (float) quest.act);
        scores.put("XP", (float) quest.startingXP);
        scores.put("Gold", (float) quest.gold);

        float fitness = fitness(scores);
        scores.put("Fitness", fitness);

        HashMap<String, Boolean> failures = checkFeasible(scores);
        co.increaseFailureCount(failures);
        boolean feasible = failures.get("Feasible");
        float f = feasible ? 1f : 0f;
        scores.put("Feasible", f);
        co.print(feasible ? "Offspring " + co.nowServing + ": Feasible (Fitness: " + fitness + ")" : "Offspring " + co.nowServing + ": Infeasible; " + " (Fitness: " + fitness + ")");

        return scores;
    }

    HashMap<String, Boolean> checkFeasible(HashMap<String, Float> scores) throws InterruptedException, InvocationTargetException {

        boolean feasible = true;
        HashMap<String, Boolean> failures = new HashMap<>();

        // Connectedness Check
        if (scores.get("Connectedness") < 1f) {
            failures.put("Connectedness", false);
            feasible = false;
        }
        else
            failures.put("Connectedness", true);

        // Free Edge Failure
        if (scores.get("Free Edges") > 0f) {
            failures.put("Free Edges", false);
            feasible = false;
        }
        else
            failures.put("Free Edges", true);

        // Geometry Check
        if (scores.get("Geometry") < 1f) {
            failures.put("Geometry", false);
            feasible = false;
        }
        else
            failures.put("Geometry", true);

        // No Repeating Monsters Check
        if (scores.get("Spawning") < 1f) {
            failures.put("Spawning", false);
            feasible = false;
        }
        else
            failures.put("Spawning", true);

        // Consistency Check
        if (scores.get("Consistency") < 1f) {
            failures.put("Consistency", false);
            feasible = false;
        }
        else
            failures.put("Consistency", true);

        // Board Size Check
        float size = scores.get("Size");
        if (size > SIZE_MAX || size < SIZE_MIN) {
            failures.put("Size", false);
            feasible = false;
        }
        else
            failures.put("Size", true);

        // Monster Group Check
        float groups = scores.get("Groups");
        if (groups > GROUP_MAX || groups < GROUP_MIN) {
            failures.put("Groups", false);
            feasible = false;
        }
        else
            failures.put("Groups", true);

        failures.put("Feasible", feasible);

        return failures;
    }

    private Pair<int[][], Integer> createBoard(CreateOffspring co, PCGBoard quest) {

        // Put together the master grid board
        // Find maximum board width and height, if all were put together side by side

        Map<Integer, GridBoard> tiles = new HashMap<>(); // Reference the tiles used
        Map<String, Map<Vector2D, Vector2D>> gridReferences = new HashMap<>(); // Reference to grid coordinates of tile placed at that position

        int width = 0;
        int height = 0;
        for (PCGNode node : quest.board) {
            // Find width of this tile, according to orientation
            String name = node.name;
            GridBoard tile = co.getTileByName(name);
            if (tile != null) {
                tile.setProperty(new PropertyInt("orientation", node.orientation));
                gridReferences.put(name, new HashMap<>());
                tiles.put(tile.getComponentID(), tile);
                int orientation = node.orientation;
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
        HashMap<PCGNode, PCGNode> drawn = new HashMap<>();  // Keeps track of which tiles have been added to the board already, for recursive purposes

        // StartX / Y Will need to be adjusted to not draw on top of existing things

        // Find first tile, as board node in the board configuration graph board
        PCGNode firstTile = quest.board.get(0);
        String startingTileName = quest.heroStartingPosition;
        for (PCGNode node : quest.board) {
            if (node.name.equals(startingTileName)) {
                firstTile = node;
                break;
            }
        }
        // System.out.println("First tile:" + firstTile.getComponentName());
        if (firstTile != null) {
            // Find grid board of first tile, rotate to correct orientation and add its tiles to the board
            GridBoard tile = co.getTileByName(firstTile.name);
            assert tile != null;
            tile.setComponentName(firstTile.name);
            int orientation = firstTile.orientation;
            Component[][] rotated = tile.rotate(orientation);
            int startX = width / 2 - rotated[0].length / 2;
            int startY = height / 2 - rotated.length / 2;
            // Bounds will keep track of where tiles actually exist in the master board, to trim to size later
            Rectangle bounds = new Rectangle(startX, startY, rotated[0].length, rotated.length);
            // Recursive call, will add all tiles in relation to their neighbours as per the board configuration
            addTilesToBoard(co, quest.board, null, firstTile, startX, startY, board, null, GenerateBoards.tiles, tileReferences, gridReferences, drawn, bounds, null);

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

            GridBoard finalBoard = new GridBoard(trimBoard);
            tileReferences = trimTileRef;
            for (Map.Entry<String, Map<Vector2D, Vector2D>> e : gridReferences.entrySet()) {
                for (Vector2D v : e.getValue().keySet()) {
                    v.subtract(bounds.x, bounds.y);
                }
            }
            if (co.nowGenerating) {
                if (!co.boards.containsKey(co.nowServing)) {
                    co.boards.put(co.nowServing, finalBoard);
                    co.boardTiles.put(co.nowServing, tiles);
                    co.tileRefs.put(co.nowServing, tileReferences);
                    co.gridRefs.put(co.nowServing, gridReferences);
                    }
            }

            return new Pair<>(trimTileRef, sizeCounter);
        }
        return null;
    }

    private void addTilesToBoard(CreateOffspring co, List<PCGNode> nodes, PCGNode parentTile, PCGNode tileToAdd, int x, int y,
                                 BoardNode[][] board,
                                 BoardNode[][] tileGrid,
                                 List<GridBoard> tiles,
                                 int[][] tileReferences, Map<String, Map<Vector2D, Vector2D>> gridReferences,
                                 Map<PCGNode, PCGNode> drawn,
                                 Rectangle bounds,
                                 String sideWithOpening) {
        if (!drawn.containsKey(parentTile) || !drawn.get(parentTile).equals(tileToAdd)) {
            // Draw this tile in the big board at x, y location
            GridBoard tile = co.getTileByName(tileToAdd.name);
            tile.setComponentName(tileToAdd.name);
            BoardNode[][] originalTileGrid = tile.rotate(tileToAdd.orientation);
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
                    board[i][j].setProperty(new PropertyInt("connections", (tileToAdd.nodeID)));

                    // Don't keep references for edge tiles
                    if (board[i][j] == null || board[i][j].getComponentName().equals("edge")
                            || board[i][j].getComponentName().equals("open")) continue;

                    // Set references
                    tileReferences[i][j] = (tile.getComponentID()+1);
                    for (String s : gridReferences.keySet()) {
                        gridReferences.get(s).remove(new Vector2D(j, i));
                    }
                    gridReferences.get(tile.getComponentName()).put(new Vector2D(j, i), new Vector2D(j - x, i - y));
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
            for (Connection c : tileToAdd.neighbours.keySet()) {
                String target = tileToAdd.neighbours.get(c);
                PCGNode neighbour = null;
                for (PCGNode node : nodes) {
                    if (node.name.equals(target)) {
                        neighbour = node;
                        break;
                    }
                }
                assert neighbour != null;
                if (drawn.containsKey(neighbour)) continue;

                // Find location to start drawing neighbour
                Pair<String, Vector2D> connectionToNeighbour = findConnection(tileToAdd, neighbour, findOpenings(tileGrid));

                if (connectionToNeighbour != null) {
                    connectionToNeighbour.b.add(x, y);
                    // Find orientation and opening connection from neighbour, generate top-left corner of neighbour from that
                    GridBoard tileN = co.getTileByName(neighbour.name);
                    if (tileN != null) {
                        BoardNode[][] tileGridN = tileN.rotate(neighbour.orientation);

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
                            addTilesToBoard(co, nodes, tileToAdd, neighbour, topLeftCorner.getX(), topLeftCorner.getY(), board, tileGridN,
                                    tiles, tileReferences, gridReferences, drawn, bounds, side);
                        }
                    }
                }
            }
        }
    }

    private Pair<String, Vector2D> findConnection(PCGNode from, PCGNode to, HashMap<String, ArrayList<Vector2D>> openings) {
        List<String> neighbours = new ArrayList<>();
        List<Connection> connections = new ArrayList<>();

        for (Connection c : from.neighbours.keySet()) {
            connections.add(c);
            neighbours.add(from.neighbours.get(c));
        }

        for (int i = 0; i < neighbours.size(); i++) {
            if (neighbours.get(i).equalsIgnoreCase(to.name)) {
                String side = switch (connections.get(i)) {
                    case NORTH -> "N";
                    case EAST -> "E";
                    case SOUTH -> "S";
                    case WEST -> "W";
                };
                if (openings.containsKey(side)) {
                    return new Pair<>(side, openings.get(side).get(0));
                }
                break;
            }
        }
        return null;
    }

    private HashMap<String, ArrayList<Vector2D>> findOpenings(BoardNode[][] tileGrid) {
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

    private void addConnectionsAtOpeningOnSide(BoardNode[][] board, BoardNode[][] originalTileGrid,
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
