package games.descent2e.pcg_clean.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import games.descent2e.pcg_clean.domain.*;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TileCatalogLoader {
    private record TileJson(String id, int[] size, String[][] grid) {}

    public TileCatalog load(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            List<TileJson> json = new Gson().fromJson(reader, new TypeToken<List<TileJson>>() {}.getType());
            return new TileCatalog(json.stream().map(this::convert).toList());
        }
    }

    private TileDefinition convert(TileJson source) {
        int height = source.grid.length;
        int width = source.grid[0].length;
        List<Cell> cells = new ArrayList<>(width * height);
        for (String[] row : source.grid) {
            if (row.length != width) throw new IllegalArgumentException("Non-rectangular tile " + source.id);
            for (String cell : row) cells.add(Cell.fromJson(cell));
        }
        return new TileDefinition(source.id, width, height, cells, findPorts(width, height, cells));
    }

    private List<Port> findPorts(int width, int height, List<Cell> cells) {
        List<Port> ports = new ArrayList<>();
        addRuns(ports, Direction.NORTH, width, i -> new GridPoint(i, 0), cells, width);
        addRuns(ports, Direction.EAST, height, i -> new GridPoint(width - 1, i), cells, width);
        addRuns(ports, Direction.SOUTH, width, i -> new GridPoint(i, height - 1), cells, width);
        addRuns(ports, Direction.WEST, height, i -> new GridPoint(0, i), cells, width);
        return ports;
    }

    private void addRuns(List<Port> ports, Direction direction, int length,
                         java.util.function.IntFunction<GridPoint> pointAt, List<Cell> cells, int width) {
        List<GridPoint> run = new ArrayList<>();
        for (int i = 0; i <= length; i++) {
            GridPoint point = i < length ? pointAt.apply(i) : null;
            boolean open = point != null && cells.get(point.y() * width + point.x()) == Cell.OPEN;
            if (open) run.add(point);
            if (!open && !run.isEmpty()) {
                ports.add(new Port(ports.size(), direction, run));
                run = new ArrayList<>();
            }
        }
    }
}
