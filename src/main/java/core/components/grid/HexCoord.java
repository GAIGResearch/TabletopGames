package core.components.grid;

import java.util.ArrayList;
import java.util.List;

/**
 * A hex on a grid of pointy-topped hexes, in axial coordinates: q increases to the east and r to the south-east.
 * <p>
 * Directions run clockwise from east: E, SE, SW, W, NW, NE. Corners run clockwise from the top right, so the side
 * facing direction d runs from corner d to corner d + 1.
 */
public record HexCoord(int q, int r) implements GridCoord<HexCoord> {

    public static final int E = 0, SE = 1, SW = 2, W = 3, NW = 4, NE = 5;
    public static final HexCoord ORIGIN = new HexCoord(0, 0);

    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 1}, {-1, 0}, {0, -1}, {1, -1}};

    /**
     * How a rectangular array of hexes is laid out: rows are numbered down the screen, and either the odd or the even
     * rows are shifted half a hex to the right.
     */
    public enum Offset {EVEN_R, ODD_R}

    /**
     * The hex at the column and row of an array laid out with the offset. The row is r.
     */
    public static HexCoord fromOffset(int col, int row, Offset offset) {
        return new HexCoord(col - (row + shift(row, offset)) / 2, row);
    }

    /**
     * The column of the hex in an array laid out with the offset. The row is r.
     */
    public int offsetCol(Offset offset) {
        return q + (r + shift(r, offset)) / 2;
    }

    private static int shift(int row, Offset offset) {
        // row + shift is even, so halving it is exact for negative rows too
        return offset == Offset.EVEN_R ? (row & 1) : -(row & 1);
    }

    /**
     * Every hex within radius steps of the origin, in order.
     */
    public static List<HexCoord> hexagon(int radius) {
        List<HexCoord> retValue = new ArrayList<>();
        for (int r = -radius; r <= radius; r++)
            for (int q = Math.max(-radius, -r - radius); q <= Math.min(radius, -r + radius); q++)
                retValue.add(new HexCoord(q, r));
        return retValue;
    }

    /**
     * The hexes exactly radius steps from this one, clockwise from the one radius steps to the north-west. The
     * k-th hex of the ring is reached by a step in direction k / radius.
     */
    public List<HexCoord> ring(int radius) {
        List<HexCoord> retValue = new ArrayList<>(6 * radius);
        HexCoord h = this;
        for (int i = 0; i < radius; i++)
            h = h.neighbour(NW);
        for (int d = 0; d < 6; d++)
            for (int i = 0; i < radius; i++) {
                h = h.neighbour(d);
                retValue.add(h);
            }
        return retValue;
    }

    @Override
    public int nDirections() {
        return 6;
    }

    @Override
    public int first() {
        return q;
    }

    @Override
    public int second() {
        return r;
    }

    @Override
    public HexCoord neighbour(int direction) {
        int[] d = DIRECTIONS[direction];
        return new HexCoord(q + d[0], r + d[1]);
    }

    @Override
    public int distance(HexCoord other) {
        int dq = q - other.q, dr = r - other.r;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }

    @Override
    public String toString() {
        return "(" + q + "," + r + ")";
    }
}
