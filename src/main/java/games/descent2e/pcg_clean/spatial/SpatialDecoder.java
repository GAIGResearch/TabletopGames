package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.layout.*;

import java.util.*;

/** Converts absolute piece genes into an inferred port graph and atomic-cell grid. */
public final class SpatialDecoder {
    private record PortRef(PlacedTile tile, int port) {}

    private final TileCatalog tiles;
    private final PhysicalPieceCatalog pieces;
    private final int minCoordinate;
    private final int maxCoordinate;
    private final int minimumSelectedPieces;

    public SpatialDecoder(TileCatalog tiles, PhysicalPieceCatalog pieces, int minCoordinate, int maxCoordinate,
                          int minimumSelectedPieces) {
        this.tiles = tiles;
        this.pieces = pieces;
        this.minCoordinate = minCoordinate;
        this.maxCoordinate = maxCoordinate;
        this.minimumSelectedPieces = minimumSelectedPieces;
    }

    public SpatialPhenotype decode(SpatialChromosome chromosome) {
        List<String> violations = new ArrayList<>();
        List<PlacedTile> selected = selectedTiles(chromosome, violations);
        if (selected.isEmpty()) throw new IllegalArgumentException("Chromosome must select at least one piece");
        for (int missing = selected.size(); missing < minimumSelectedPieces; missing++)
            violations.add("Board is missing a required piece (minimum " + minimumSelectedPieces + ")");
        Map<Integer, GridPoint> origins = origins(chromosome, selected);
        List<TileConnection> connections = inferConnections(selected, origins, violations);
        Map<Integer, Set<Integer>> graph = graph(selected, connections);
        addConnectivityViolations(selected, graph, violations);
        addRoleViolations(selected, violations);
        Map<GridPoint, Cell> cells = render(selected, origins, violations);
        BoardGenome genome = new BoardGenome(selected, connections);
        BoardLayout layout = new BoardLayout(origins, cells, graph, violations);
        return new SpatialPhenotype(genome, layout, violations);
    }

    private List<PlacedTile> selectedTiles(SpatialChromosome chromosome, List<String> violations) {
        List<PlacedTile> result = new ArrayList<>();
        for (int i = 0; i < chromosome.genes().size(); i++) {
            PieceGene gene = chromosome.genes().get(i);
            if (!gene.selected()) continue;
            PhysicalPiece piece = pieces.pieces().get(i);
            if (gene.face() < 0 || gene.face() >= piece.faces().size()) {
                violations.add("Piece " + piece.id() + " has invalid face " + gene.face());
                continue;
            }
            result.add(new PlacedTile(i, piece.faces().get(gene.face()), gene.quarterTurns()));
        }
        return result;
    }

    private Map<Integer, GridPoint> origins(SpatialChromosome chromosome, List<PlacedTile> selected) {
        Map<Integer, GridPoint> result = new LinkedHashMap<>();
        selected.forEach(tile -> {
            PieceGene gene = chromosome.genes().get(tile.instanceId());
            result.put(tile.instanceId(), new GridPoint(gene.x(), gene.y()));
        });
        return result;
    }

    private List<TileConnection> inferConnections(List<PlacedTile> selected, Map<Integer, GridPoint> origins,
                                                   List<String> violations) {
        List<PortRef> ports = selected.stream().flatMap(tile -> tiles.require(tile.tileId())
                .rotate(tile.quarterTurns()).ports().stream().map(port -> new PortRef(tile, port.index()))).toList();
        List<TileConnection> edges = new ArrayList<>();
        Map<PortRef, Integer> uses = new HashMap<>();
        for (int i = 0; i < ports.size(); i++) for (int j = i + 1; j < ports.size(); j++) {
            PortRef a = ports.get(i);
            PortRef b = ports.get(j);
            if (a.tile.instanceId() == b.tile.instanceId()) continue;
            if (PortGeometry.aligned(a.tile, origins.get(a.tile.instanceId()), a.port,
                    b.tile, origins.get(b.tile.instanceId()), b.port, tiles)) {
                edges.add(new TileConnection(a.tile.instanceId(), a.port, b.tile.instanceId(), b.port));
                uses.merge(a, 1, Integer::sum);
                uses.merge(b, 1, Integer::sum);
            }
        }
        ports.forEach(port -> {
            int count = uses.getOrDefault(port, 0);
            if (count == 0) violations.add("Unconnected port " + port.port + " on piece " + label(port.tile));
            if (count > 1) violations.add("Port " + port.port + " connects to multiple pieces on " + label(port.tile));
        });
        return edges;
    }

    private Map<Integer, Set<Integer>> graph(List<PlacedTile> selected, List<TileConnection> connections) {
        Map<Integer, Set<Integer>> result = new LinkedHashMap<>();
        selected.forEach(tile -> result.put(tile.instanceId(), new LinkedHashSet<>()));
        connections.forEach(edge -> {
            result.get(edge.firstTile()).add(edge.secondTile());
            result.get(edge.secondTile()).add(edge.firstTile());
        });
        return result;
    }

    private void addConnectivityViolations(List<PlacedTile> selected, Map<Integer, Set<Integer>> graph,
                                           List<String> violations) {
        Set<Integer> reached = new HashSet<>();
        visit(selected.get(0).instanceId(), graph, reached);
        selected.stream().map(PlacedTile::instanceId).filter(id -> !reached.contains(id))
                .forEach(id -> violations.add("Piece " + label(selected, id)
                        + " is disconnected from the main component"));
    }

    private void visit(int node, Map<Integer, Set<Integer>> graph, Set<Integer> reached) {
        if (!reached.add(node)) return;
        graph.getOrDefault(node, Set.of()).forEach(next -> visit(next, graph, reached));
    }

    private void addRoleViolations(List<PlacedTile> selected, List<String> violations) {
        addExactRoleViolation(selected, "entrance", violations);
        addExactRoleViolation(selected, "exit", violations);
    }

    private void addExactRoleViolation(List<PlacedTile> selected, String role, List<String> violations) {
        long count = selected.stream().filter(tile -> tile.tileId().toLowerCase(Locale.ROOT).startsWith(role)).count();
        if (count != 1) violations.add("Expected exactly one " + role + " but found " + count);
    }

    private Map<GridPoint, Cell> render(List<PlacedTile> selected, Map<Integer, GridPoint> origins,
                                        List<String> violations) {
        Map<GridPoint, Cell> cells = new LinkedHashMap<>();
        Map<GridPoint, PlacedTile> owners = new LinkedHashMap<>();
        selected.forEach(placement -> renderTile(placement, origins.get(placement.instanceId()),
                cells, owners, violations));
        return cells;
    }

    private void renderTile(PlacedTile placement, GridPoint origin, Map<GridPoint, Cell> cells,
                            Map<GridPoint, PlacedTile> owners, List<String> violations) {
        RotatedTile tile = tiles.require(placement.tileId()).rotate(placement.quarterTurns());
        for (int y = 0; y < tile.height(); y++) for (int x = 0; x < tile.width(); x++)
            renderCell(placement, origin, new GridPoint(x, y), tile.cellAt(x, y), cells, owners, violations);
    }

    private void renderCell(PlacedTile placement, GridPoint origin, GridPoint local, Cell cell,
                            Map<GridPoint, Cell> cells, Map<GridPoint, PlacedTile> owners,
                            List<String> violations) {
        if (cell == Cell.VOID) return;
        GridPoint point = origin.plus(local);
        if (!inBounds(point)) violations.add("Piece " + label(placement) + " extends out of bounds at " + point);
        Cell previous = cells.putIfAbsent(point, cell);
        PlacedTile previousOwner = owners.putIfAbsent(point, placement);
        if (previous != null && !(previous == Cell.OPEN && cell == Cell.OPEN))
            violations.add("Pieces " + label(previousOwner) + " and " + label(placement)
                    + " overlap at " + point);
    }

    private boolean inBounds(GridPoint point) {
        return point.x() >= minCoordinate && point.x() <= maxCoordinate
                && point.y() >= minCoordinate && point.y() <= maxCoordinate;
    }

    private String label(PlacedTile tile) { return tile.instanceId() + ":" + tile.tileId(); }

    private String label(List<PlacedTile> selected, int id) {
        return selected.stream().filter(tile -> tile.instanceId() == id).findFirst()
                .map(this::label).orElse(Integer.toString(id));
    }
}
