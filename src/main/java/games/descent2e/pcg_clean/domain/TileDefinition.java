package games.descent2e.pcg_clean.domain;

import java.util.ArrayList;
import java.util.List;

/** Immutable physical tile template loaded from tiles.json. */
public record TileDefinition(String id, int width, int height, List<Cell> cells, List<Port> ports) {
    public TileDefinition {
        cells = List.copyOf(cells);
        ports = List.copyOf(ports);
        if (width <= 0 || height <= 0 || cells.size() != width * height)
            throw new IllegalArgumentException("Invalid tile dimensions for " + id);
    }

    public Cell cellAt(int x, int y) { return cells.get(y * width + x); }

    public RotatedTile rotate(int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        int rotatedWidth = turns % 2 == 0 ? width : height;
        int rotatedHeight = turns % 2 == 0 ? height : width;
        List<Cell> rotated = new ArrayList<>(java.util.Collections.nCopies(rotatedWidth * rotatedHeight, Cell.VOID));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                GridPoint p = rotatePoint(new GridPoint(x, y), turns);
                rotated.set(p.y() * rotatedWidth + p.x(), cellAt(x, y));
            }
        }
        List<Port> rotatedPorts = ports.stream()
                .map(port -> new Port(port.index(), port.direction().rotate(turns),
                        port.cells().stream().map(p -> rotatePoint(p, turns)).toList()))
                .toList();
        return new RotatedTile(id, rotatedWidth, rotatedHeight, rotated, rotatedPorts);
    }

    private GridPoint rotatePoint(GridPoint p, int turns) {
        return switch (turns) {
            case 0 -> p;
            case 1 -> new GridPoint(height - 1 - p.y(), p.x());
            case 2 -> new GridPoint(width - 1 - p.x(), height - 1 - p.y());
            case 3 -> new GridPoint(p.y(), width - 1 - p.x());
            default -> throw new IllegalStateException();
        };
    }
}
