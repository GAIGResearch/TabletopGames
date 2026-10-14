package core.components.grid;

/**
 * A square on a grid, with x increasing to the east and y to the south. Directions run clockwise from east: E, S, W,
 * N.
 */
public record SquareCoord(int x, int y) implements GridCoord<SquareCoord> {

    public static final int E = 0, S = 1, W = 2, N = 3;

    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    @Override
    public int nDirections() {
        return 4;
    }

    @Override
    public int first() {
        return x;
    }

    @Override
    public int second() {
        return y;
    }

    @Override
    public SquareCoord neighbour(int direction) {
        int[] d = DIRECTIONS[direction];
        return new SquareCoord(x + d[0], y + d[1]);
    }

    @Override
    public int distance(SquareCoord other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
