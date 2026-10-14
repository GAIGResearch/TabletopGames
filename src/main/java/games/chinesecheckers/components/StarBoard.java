package games.chinesecheckers.components;

import core.CoreConstants;
import core.components.Component;
import core.components.grid.HexCoord;
import core.components.grid.SparseGrid;
import games.chinesecheckers.CCParameters;

import java.util.*;

/**
 * The star-shaped board: 121 nodes, numbered row by row from the top point, and the pegs on them.
 * <p>
 * The nodes are the hexes of a hexagram round the origin: those with every cube coordinate at least -RADIUS, or every
 * one at most RADIUS. The shape and the base colours of the nodes are the same on every board; only the pegs change.
 */
public class StarBoard extends Component {

    // the half-width of the hexagon in the middle of the star
    public static final int RADIUS = 4;
    private static final List<HexCoord> POSITIONS = new ArrayList<>();
    private static final Map<HexCoord, Integer> NODES = new HashMap<>();
    private static final Peg.Colour[] BASE_COLOURS;

    static {
        for (HexCoord h : HexCoord.hexagon(2 * RADIUS)) {
            int q = h.q(), r = h.r(), s = -q - r;
            if (Math.min(q, Math.min(r, s)) >= -RADIUS || Math.max(q, Math.max(r, s)) <= RADIUS)
                POSITIONS.add(h);
        }
        // HexCoord.hexagon lists hexes row by row
        for (int i = 0; i < POSITIONS.size(); i++)
            NODES.put(POSITIONS.get(i), i);

        BASE_COLOURS = new Peg.Colour[POSITIONS.size()];
        Arrays.fill(BASE_COLOURS, Peg.Colour.neutral);
        CCParameters params = new CCParameters();
        for (Map.Entry<Peg.Colour, int[]> e : params.colourIndices.entrySet())
            for (int i : e.getValue())
                BASE_COLOURS[i] = e.getKey();
    }

    private SparseGrid<HexCoord, Peg> pegs;

    public StarBoard() {
        super(CoreConstants.ComponentType.BOARD, "StarBoard");
        pegs = new SparseGrid<>("Pegs");
    }

    private StarBoard(StarBoard orig) {
        super(CoreConstants.ComponentType.BOARD, orig.componentName, orig.componentID);
    }

    public int nNodes() {
        return POSITIONS.size();
    }

    public HexCoord getPosition(int node) {
        return POSITIONS.get(node);
    }

    /**
     * The node at the hex, or -1 if it is off the board.
     */
    public int getNode(HexCoord hex) {
        Integer node = NODES.get(hex);
        return node == null ? -1 : node;
    }

    /**
     * The colour of the node: that of the player whose destination it is, or neutral.
     */
    public Peg.Colour getBaseColour(int node) {
        return BASE_COLOURS[node];
    }

    /**
     * The peg on the node, or null if it is empty.
     */
    public Peg getPeg(int node) {
        return pegs.get(POSITIONS.get(node));
    }

    public boolean isOccupied(int node) {
        return pegs.contains(POSITIONS.get(node));
    }

    /**
     * Puts the peg on the node, or empties it if the peg is null.
     */
    public void setPeg(int node, Peg peg) {
        pegs.set(POSITIONS.get(node), peg);
    }

    /**
     * The number of steps between the nodes, moving between adjacent ones.
     */
    public int distanceBetween(int from, int to) {
        // a straight path between two hexes of the star stays on it, so this is the distance between the hexes
        return POSITIONS.get(from).distance(POSITIONS.get(to));
    }

    @Override
    public StarBoard copy() {
        StarBoard retValue = new StarBoard(this);
        retValue.pegs = pegs.copy();
        copyComponentTo(retValue);
        return retValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof StarBoard other) {
            return componentID == other.componentID && other.pegs.equals(pegs);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID, pegs);
    }
}
