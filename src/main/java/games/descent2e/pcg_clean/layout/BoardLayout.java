package games.descent2e.pcg_clean.layout;

import games.descent2e.pcg_clean.domain.Cell;
import games.descent2e.pcg_clean.domain.GridPoint;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record BoardLayout(Map<Integer, GridPoint> origins, Map<GridPoint, Cell> cells,
                          Map<Integer, Set<Integer>> graph, List<String> problems) {
    public BoardLayout {
        // Map.copyOf/Set.copyOf do not preserve iteration order. Graph traversal order
        // feeds the repair operator, so losing it makes fixed-seed runs diverge between JVMs.
        origins = Collections.unmodifiableMap(new LinkedHashMap<>(origins));
        cells = Collections.unmodifiableMap(new LinkedHashMap<>(cells));
        Map<Integer, Set<Integer>> orderedGraph = new LinkedHashMap<>();
        graph.forEach((id, neighbours) -> orderedGraph.put(id,
                Collections.unmodifiableSet(new LinkedHashSet<>(neighbours))));
        graph = Collections.unmodifiableMap(orderedGraph);
        problems = List.copyOf(problems);
    }

    public boolean assembled() { return problems.isEmpty(); }
    public long traversableCellCount() { return cells.values().stream().filter(Cell::traversable).count(); }
}
