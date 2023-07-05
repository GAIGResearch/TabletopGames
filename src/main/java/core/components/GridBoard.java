package core.components;

import core.CoreConstants;
import core.interfaces.IComponentContainer;
import core.properties.PropertyString;
import core.properties.PropertyVector2D;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;
import utilities.Vector2D;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.imgHash;
import static utilities.Utils.getNeighbourhood;

public class GridBoard<T extends Component> extends Component implements IComponentContainer<T> {

    private int width;  // Width of the board
    private int height;  // Height of the board

    private Component[][] grid;  // 2D grid representation of this board

    protected GridBoard() {
        super(CoreConstants.ComponentType.BOARD);
    }

    public GridBoard(int width, int height) {
        super(CoreConstants.ComponentType.BOARD);
        this.width = width;
        this.height = height;
        this.grid = new Component[height][width];
    }

    public GridBoard(int width, int height, T defaultValue) {
        this(width, height);
        for (int y = 0; y < height; y++)
            Arrays.fill(grid[y], defaultValue);
    }

    public GridBoard(Component[][] grid) {
        super(CoreConstants.ComponentType.BOARD);
        this.width = grid[0].length;
        this.height = grid.length;
        this.grid = grid;
    }

    protected GridBoard(Component[][] grid, int ID) {
        super(CoreConstants.ComponentType.BOARD, ID);
        this.width = grid[0].length;
        this.height = grid.length;
        this.grid = grid;
    }

    protected GridBoard(int width, int height, int ID) {
        super(CoreConstants.ComponentType.BOARD, ID);
        this.width = width;
        this.height = height;
        this.grid = new Component[height][width];
    }

    public GridBoard(GridBoard<T> orig) {
        super(CoreConstants.ComponentType.BOARD);
        this.width = orig.getWidth();
        this.height = orig.getHeight();
        this.grid = orig.grid.clone();
    }

    /**
     * Get the width and height of this grid.
     */
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Set the width or height of the grid. Creates a new grid with the new dimensions, and copies elements from the
     * previous grid that fit in the new one (same x, y coordinates). Possibly adding an offset from which to add
     * elements in the new grid.
     */
    public void setWidth(int width) {
        setWidthHeight(width, height, 0, 0);
    }

    public void setHeight(int height) {
        setWidthHeight(width, height, 0, 0);
    }

    public void setWidth(int width, int offset) {
        setWidthHeight(width, height, offset, 0);
    }

    public void setHeight(int height, int offset) {
        setWidthHeight(width, height, 0, offset);
    }

    public void setWidthHeight(int width, int height) {
        setWidthHeight(width, height, 0, 0);
    }

    public void setWidthHeight(int width, int height, int offsetX, int offsetY) {
        if (offsetX + this.width > width) offsetX = 0;
        if (offsetY + this.height > height) offsetY = 0;

        int w = Math.min(width, this.width);
        int h = Math.min(height, this.height);

        this.width = width;
        this.height = height;

        Component[][] grid = new Component[height][width];
        for (int i = 0; i < h; i++) {
            if (w >= 0) System.arraycopy(this.grid[i], 0, grid[i + offsetY], offsetX, w);
        }
        this.grid = grid;
    }

    /**
     * Sets the element at position (x, y).
     *
     * @param x     - x coordinate in the grid.
     * @param y     - y coordinate in the grid.
     * @param value - new value for this element.
     * @return - true if coordinates in bounds, false otherwise (and function fails).
     */
    public boolean setElement(int x, int y, T value) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[y][x] = value;
            return true;
        } else
            return false;
    }

    public boolean setElement(Vector2D pos, T value){
        return setElement(pos.getX(), pos.getY(), value);
    }

    /**
     * Retrieves the element at position (x, y).
     *
     * @param x - x coordinate in the grid.
     * @param y - y coordinate in the grid.
     * @return - element at (x,y) in the grid.
     */
    public T getElement(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height)
            return (T)grid[y][x];
        return null;
    }

    public T getElement(Vector2D pos) {
        return getElement(pos.getX(), pos.getY());
    }

    /**
     * Retrieves the grid.
     *
     * @return - 2D grid.
     */
    public Component[][] getGridValues() {
        return grid;
    }

    public List<Vector2D> getEmptyCells(T defaultElement) {
        List<Vector2D> emptyCells = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (getElement(j, i) == null || getElement(j, i).equals(defaultElement)) {
                    emptyCells.add(new Vector2D(j, i));
                }
            }
        }
        return emptyCells;
    }

    /**
     * Returns a new grid, copy of this one, with given orientation.
     *
     * @param orientation - int orientation, how many times it should be rotated clockwise
     * @return - new grid with the same elements and correct orientation.
     */
    public Component[][] rotate(int orientation) {
        GridBoard<T> copy = copy();
        orientation %= 4;  // Maximum 4 sides to a grid
        for (int i = 0; i < orientation; i++) {
            copy.grid = rotateClockWise(copy.grid);
        }
        return copy.grid;
    }

    /**
     * Rotates a given grid clockwise, returning new one
     *
     * @param original - original grid to rotate
     * @return rotated grid
     */
    private Component[][] rotateClockWise(Component[][] original) {
        final int M = original.length;
        final int N = original[0].length;
        Component[][] grid = new Component[N][M];
        for (int r = 0; r < M; r++) {
            for (int c = 0; c < N; c++) {
                grid[c][M - 1 - r] = original[r][c];
            }
        }
        return grid;
    }

    /**
     * Returns a 1D representation of this grid, one row after another
     *
     * @return 1D flattened grid
     */
    public Component[] flattenGrid() {
        int length = getHeight() * getWidth();
        Component[] array = new Component[length];
        for (int i = 0; i < getHeight(); i++) {
            System.arraycopy(grid[i], 0, array, i * getWidth(), grid[i].length);
        }
        return array;
    }

    @Override
    public GridBoard<T> copy() {
        Component[][] gridCopy = new Component[getHeight()][getWidth()];
        for (int i = 0; i < height; i++) {
            if (width >= 0) System.arraycopy(grid[i], 0, gridCopy[i], 0, width);
        }
        GridBoard<T> g = new GridBoard<>(gridCopy, componentID);
        copyComponentTo(g);
        return g;
    }

    public GridBoard<T> emptyCopy() {
        GridBoard<T> g = new GridBoard<>(getWidth(), getHeight(), componentID);
        copyComponentTo(g);
        return g;
    }

    @Override
    public String toString() {
        String s = "";
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                T t = getElement(x, y);
                s += t.toString() + " ";
            }
            s += "\n";
        }
        return s;
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

        this.grid = new Component[height][width];

        JSONArray grids = (JSONArray) board.get("grid");
        int y = 0;
        for (Object g : grids) {
            if (((JSONArray) g).get(0) instanceof JSONArray) {
                y = 0;
                for (Object o : (JSONArray) g) {
                    JSONArray row = (JSONArray) o;
                    int x = 0;
                    for (Object o1 : row) {
                        setElement(x, y, (T) o1);
                        x++;
                    }
                    y++;
                }
            } else {
                JSONArray row = (JSONArray) g;
                int x = 0;
                for (Object o1 : row) {
                    setElement(x, y, (T) o1);
                    x++;
                }
                y++;
            }
        }
    }

    /**
     * Generates a graph from this grid, with 4-way or 8-way connectivity.
     *
     * @param way8 - if true, the board has 8-way connectivity, otherwise 4-way.
     * @return - GraphBoard, board with board nodes connected. All board nodes have information about their location
     * in the original grid, via the "coordinates" property.
     */
    public GraphBoard toGraphBoard(boolean way8) {
        GraphBoard gb = new GraphBoard(componentName, componentID);
        HashMap<Vector2D, BoardNode> bnMapping = new HashMap<>();
        // Add all cells as board nodes connected to each other
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                BoardNode bn = new BoardNode(-1, getElement(j, i).toString());
                bn.setProperty(new PropertyVector2D("coordinates", new Vector2D(j, i)));
                bn.setProperty(new PropertyString("terrain", getElement(j, i).toString()));
                gb.addBoardNode(bn);
                bnMapping.put(new Vector2D(j, i), bn);
            }
        }

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                BoardNode bn = bnMapping.get(new Vector2D(j, i));

                // Add neighbours
                List<Vector2D> neighbours = getNeighbourhood(j, i, width, height, way8);
                for (Vector2D neighbour : neighbours) {
                    BoardNode bn2 = bnMapping.get(neighbour);
                    gb.addConnection(bn, bn2);
                }
            }
        }
        return gb;
    }

    /**
     * Generates a graph from this grid, with 4-way or 8-way connectivity and pre-set neighbouring cells.
     * Used to restrict grid connectivity further.
     *
     * @param neighbours - list of neighbouring cells, where each Vector2D is coordinates to a cell in the grid.
     * @return - GraphBoard, board with board nodes connected. All board nodes have information about their location
     * in the original grid, via the "coordinates" property.
     */
    public GraphBoard toGraphBoard(List<Pair<Vector2D, Vector2D>> neighbours) {
        GraphBoard gb = new GraphBoard(componentName, componentID);
        HashMap<Vector2D, BoardNode> bnMapping = new HashMap<>();
        // Add all cells as board nodes connected to each other
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (getElement(j, i) != null) {
                    BoardNode bn = new BoardNode(-1, getElement(j, i).toString());
                    bn.setProperty(new PropertyVector2D("coordinates", new Vector2D(j, i)));
                    bn.setProperty(new PropertyString("terrain", getElement(j, i).toString()));
                    gb.addBoardNode(bn);
                    bnMapping.put(new Vector2D(j, i), bn);
                }
            }
        }
        for (Pair<Vector2D, Vector2D> p : neighbours) {
            if (bnMapping.get(p.a) != null && bnMapping.get(p.b) != null) {
                gb.addConnection(bnMapping.get(p.a), bnMapping.get(p.b));
            }
        }
        return gb;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GridBoard) {
            GridBoard<?> other = (GridBoard<?>) o;
            return componentID == other.componentID && Arrays.equals(flattenGrid(), other.flattenGrid());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID) + 5 * Arrays.hashCode(flattenGrid());
    }

    @Override
    public List<T> getComponents() {
        return Arrays.stream(flattenGrid()).map( component -> (T) component).collect(Collectors.toList());
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }
}
