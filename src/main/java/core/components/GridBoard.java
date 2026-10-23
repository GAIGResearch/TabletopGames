package core.components;

import core.CoreConstants;
import core.components.grid.SparseGrid;
import core.components.grid.SquareCoord;
import core.interfaces.IComponentContainer;
import core.properties.PropertyString;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Vector2D;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static core.CoreConstants.imgHash;

/**
 * A board of width by height squares, each holding a BoardNode or empty: a facade over a SparseGrid of SquareCoords
 * that bounds it and reads and writes it by (x, y), with x increasing to the east and y to the south.
 * <p>
 * Unlike the values of a SparseGrid, BoardNodes may be changed in place (their properties and neighbours), so a copy
 * of the board copies its nodes, with BoardNode.copy(). A node that is immutable (and has no neighbours) may return
 * itself from copy(), to be shared by the copies of the board.
 */
public class GridBoard extends Component implements IComponentContainer<BoardNode> {

    private int width, height;
    private SparseGrid<SquareCoord, BoardNode> grid;

    // for loadBoard
    private GridBoard() {
        super(CoreConstants.ComponentType.BOARD);
        grid = new SparseGrid<>(componentName);
    }

    public GridBoard(int width, int height) {
        this(width, height, "GridBoard");
    }

    public GridBoard(int width, int height, String name) {
        super(CoreConstants.ComponentType.BOARD, name);
        this.width = width;
        this.height = height;
        grid = new SparseGrid<>(name);
    }

    /**
     * A board with the given componentID, as when restoring a saved state.
     */
    public GridBoard(int width, int height, String name, int componentID) {
        super(CoreConstants.ComponentType.BOARD, name, componentID);
        this.width = width;
        this.height = height;
        grid = new SparseGrid<>(name);
    }

    /**
     * A board holding the nodes of the array, indexed [y][x].
     */
    public GridBoard(BoardNode[][] nodes) {
        this(nodes[0].length, nodes.length);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                setElement(x, y, nodes[y][x]);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Resizes the board, keeping the nodes that still fit after moving them by (offsetX, offsetY). An offset that
     * would push the old board past the new one's edge is taken as 0.
     */
    public void setWidthHeight(int width, int height, int offsetX, int offsetY) {
        if (offsetX + this.width > width) offsetX = 0;
        if (offsetY + this.height > height) offsetY = 0;
        Map<SquareCoord, BoardNode> old = new HashMap<>(grid.asMap());
        this.width = width;
        this.height = height;
        grid = new SparseGrid<>(componentName);
        for (Map.Entry<SquareCoord, BoardNode> e : old.entrySet()) {
            int x = e.getKey().x() + offsetX, y = e.getKey().y() + offsetY;
            if (inBounds(x, y))
                setElement(x, y, e.getValue());
        }
    }

    public void setWidthHeight(int width, int height) {
        setWidthHeight(width, height, 0, 0);
    }

    /**
     * The node at (x, y), or null if the square is empty or off the board.
     */
    public BoardNode getElement(int x, int y) {
        return grid.get(new SquareCoord(x, y));
    }

    public BoardNode getElement(SquareCoord pos) {
        return grid.get(pos);
    }

    public BoardNode getElement(Vector2D pos) {
        return getElement(pos.getX(), pos.getY());
    }

    /**
     * Puts the node at (x, y), or empties the square if it is null.
     *
     * @return true, or false (changing nothing) if (x, y) is off the board
     */
    public boolean setElement(int x, int y, BoardNode value) {
        if (!inBounds(x, y))
            return false;
        grid.set(new SquareCoord(x, y), value);
        return true;
    }

    public boolean setElement(SquareCoord pos, BoardNode value) {
        return setElement(pos.x(), pos.y(), value);
    }

    public boolean setElement(Vector2D pos, BoardNode value) {
        return setElement(pos.getX(), pos.getY(), value);
    }

    public boolean contains(int x, int y) {
        return grid.contains(new SquareCoord(x, y));
    }

    /**
     * The number of squares holding a node.
     */
    public int size() {
        return grid.size();
    }

    public boolean isEmpty() {
        return grid.isEmpty();
    }

    /**
     * A read-only view of the nodes by position, row by row.
     */
    public Map<SquareCoord, BoardNode> asMap() {
        return grid.asMap();
    }

    /**
     * The board's nodes in an array indexed [y][x], turned clockwise by a number of quarter turns. The nodes are
     * copies.
     */
    public BoardNode[][] rotate(int orientation) {
        BoardNode[][] retValue = copy().toArray();
        for (int i = 0; i < orientation % 4; i++)
            retValue = rotateClockWise(retValue);
        return retValue;
    }

    private static BoardNode[][] rotateClockWise(BoardNode[][] original) {
        final int M = original.length;
        final int N = original[0].length;
        BoardNode[][] grid = new BoardNode[N][M];
        for (int r = 0; r < M; r++) {
            for (int c = 0; c < N; c++) {
                grid[c][M - 1 - r] = original[r][c];
            }
        }
        return grid;
    }

    private BoardNode[][] toArray() {
        BoardNode[][] retValue = new BoardNode[height][width];
        for (Map.Entry<SquareCoord, BoardNode> e : grid.asMap().entrySet())
            retValue[e.getKey().y()][e.getKey().x()] = e.getValue();
        return retValue;
    }

    /**
     * The squares row by row, null where one is empty.
     */
    public BoardNode[] flattenGrid() {
        BoardNode[] retValue = new BoardNode[width * height];
        for (Map.Entry<SquareCoord, BoardNode> e : grid.asMap().entrySet())
            retValue[e.getKey().y() * width + e.getKey().x()] = e.getValue();
        return retValue;
    }

    @Override
    public GridBoard copy() {
        GridBoard g = new GridBoard(width, height, componentName, componentID);
        copyNodesTo(g);
        copyComponentTo(g);
        return g;
    }

    /**
     * A copy with a new componentID. Its nodes keep theirs.
     */
    public GridBoard copyNewID() {
        GridBoard g = new GridBoard(width, height, componentName);
        copyNodesTo(g);
        copyComponentTo(g);
        return g;
    }

    // copies each node into g, with its neighbours the copies of its neighbours
    private void copyNodesTo(GridBoard g) {
        Map<Integer, BoardNode> nodeCopies = new HashMap<>();
        for (Map.Entry<SquareCoord, BoardNode> e : grid.asMap().entrySet()) {
            BoardNode copy = e.getValue().copy();
            g.grid.set(e.getKey(), copy);
            nodeCopies.put(copy.getComponentID(), copy);
        }
        for (BoardNode node : grid.asMap().values()) {
            BoardNode copy = nodeCopies.get(node.getComponentID());
            for (Map.Entry<BoardNode, Double> neighbour : node.getNeighbours().entrySet())
                copy.addNeighbourWithCost(nodeCopies.get(neighbour.getKey().getComponentID()), neighbour.getValue());
            for (Map.Entry<BoardNode, Integer> neighbour : node.getNeighbourSideMapping().entrySet())
                copy.addNeighbourOnSide(nodeCopies.get(neighbour.getKey().getComponentID()), neighbour.getValue());
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                BoardNode t = getElement(x, y);
                if (t != null) s.append(t.getComponentName()).append(" ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    /**
     * Loads all boards from a JSON file.
     *
     * @param filename - path to file.
     * @return - List of Board objects.
     */
    public static List<GridBoard> loadBoards(String filename) {
        JSONParser jsonParser = new JSONParser();
        ArrayList<GridBoard> gridBoards = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for (Object o : data) {
                GridBoard newGridBoard = new GridBoard();
                newGridBoard.loadBoard((JSONObject) o);
                gridBoards.add(newGridBoard);
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return gridBoards;
    }

    /**
     * Loads board info from a JSON file.
     *
     * @param board - board to load in JSON format
     */
    public void loadBoard(JSONObject board) {
        componentName = (String) board.get("id");

        JSONArray size = (JSONArray) board.get("size");
        this.width = (int) (long) size.get(0);
        this.height = (int) (long) size.get(1);

        if (board.get("img") != null) {
            properties.put(imgHash, new PropertyString((String) board.get("img")));
        }

        grid = new SparseGrid<>(componentName);

        JSONArray grids = (JSONArray) board.get("grid");
        int y = 0;
        for (Object g : grids) {
            if (((JSONArray) g).get(0) instanceof JSONArray) {
                y = 0;
                for (Object o : (JSONArray) g) {
                    loadRow((JSONArray) o, y);
                    y++;
                }
            } else {
                loadRow((JSONArray) g, y);
                y++;
            }
        }
    }

    private void loadRow(JSONArray row, int y) {
        int x = 0;
        for (Object o : row) {
            // TODO FIX THIS
            if (o instanceof String name)
                setElement(x, y, new BoardNode(-1, name));
            else
                setElement(x, y, (BoardNode) o);
            x++;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GridBoard other)) return false;
        return componentID == other.componentID && width == other.width && height == other.height &&
                grid.asMap().equals(other.grid.asMap());
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentID, width, height, grid.asMap());
    }

    /**
     * The nodes on the board, row by row.
     */
    @Override
    public List<BoardNode> getComponents() {
        return new ArrayList<>(grid.asMap().values());
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }
}
