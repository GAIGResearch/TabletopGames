package games.ventlife.components;

import java.util.ArrayList;
import java.util.List;

/**
 * A position in the vent field, in axial coordinates (q, r) for pointy-topped hexes: q increases to the east, r to
 * the south-east.
 */
public record Hex(int q, int r) {

    /**
     * The six directions, clockwise from east: E, SE, SW, W, NW, NE.
     */
    public static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}};

    public Hex neighbour(int direction) {
        int[] d = DIRECTIONS[direction];
        return new Hex(q + d[0], r + d[1]);
    }

    /**
     * The six neighbouring positions, in direction order.
     */
    public List<Hex> neighbours() {
        List<Hex> retValue = new ArrayList<>(6);
        for (int d = 0; d < 6; d++)
            retValue.add(neighbour(d));
        return retValue;
    }

    @Override
    public String toString() {
        return "(" + q + "," + r + ")";
    }
}
