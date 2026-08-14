package games.descent2e.pcg_clean.domain;

import java.util.List;

/** A maximal run of OPEN cells on one edge of a tile. */
public record Port(int index, Direction direction, List<GridPoint> cells) {
    public Port {
        cells = List.copyOf(cells);
        if (cells.isEmpty()) throw new IllegalArgumentException("A port needs at least one cell");
    }
}
