package games.descent2e.pcg_clean.domain;

public record GridPoint(int x, int y) {
    public GridPoint plus(GridPoint other) { return new GridPoint(x + other.x, y + other.y); }
}
