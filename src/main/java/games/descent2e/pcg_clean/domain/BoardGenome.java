package games.descent2e.pcg_clean.domain;

import java.util.List;

/** Framework-independent genotype: physical pieces and their intended port graph. */
public record BoardGenome(List<PlacedTile> tiles, List<TileConnection> connections) {
    public BoardGenome {
        tiles = List.copyOf(tiles);
        connections = List.copyOf(connections);
        if (tiles.isEmpty()) throw new IllegalArgumentException("A board needs at least one tile");
    }
}
