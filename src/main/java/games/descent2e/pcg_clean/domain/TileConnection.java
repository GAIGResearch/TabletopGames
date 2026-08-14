package games.descent2e.pcg_clean.domain;

public record TileConnection(int firstTile, int firstPort, int secondTile, int secondPort) {
    public TileConnection {
        if (firstTile == secondTile) throw new IllegalArgumentException("A tile cannot connect to itself");
    }

    public boolean contains(int tileId) { return firstTile == tileId || secondTile == tileId; }
    public int other(int tileId) {
        if (firstTile == tileId) return secondTile;
        if (secondTile == tileId) return firstTile;
        throw new IllegalArgumentException("Tile is not part of this connection");
    }
}
