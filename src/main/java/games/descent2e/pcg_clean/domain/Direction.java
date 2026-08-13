package games.descent2e.pcg_clean.domain;

public enum Direction {
    NORTH(0, -1), EAST(1, 0), SOUTH(0, 1), WEST(-1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() { return dx; }
    public int dy() { return dy; }
    public Direction opposite() { return values()[(ordinal() + 2) % 4]; }
    public Direction rotate(int quarterTurns) { return values()[Math.floorMod(ordinal() + quarterTurns, 4)]; }
}
