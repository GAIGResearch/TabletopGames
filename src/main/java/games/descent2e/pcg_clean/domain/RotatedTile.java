package games.descent2e.pcg_clean.domain;

import java.util.List;

public record RotatedTile(String tileId, int width, int height, List<Cell> cells, List<Port> ports) {
    public RotatedTile { cells = List.copyOf(cells); ports = List.copyOf(ports); }
    public Cell cellAt(int x, int y) { return cells.get(y * width + x); }
    public Port port(int index) { return ports.stream().filter(p -> p.index() == index).findFirst().orElseThrow(); }
}
