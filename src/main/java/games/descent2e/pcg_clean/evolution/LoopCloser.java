package games.descent2e.pcg_clean.evolution;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.layout.*;

import java.util.*;
import java.util.random.RandomGenerator;

/** Adds valid edges between unused ports that already coincide in the assembled grid. */
public final class LoopCloser {
    private record PortRef(PlacedTile tile, int port) {}

    private final TileCatalog catalog;

    public LoopCloser(TileCatalog catalog) { this.catalog = catalog; }

    public BoardGenome closeAvailableLoops(BoardGenome genome, RandomGenerator random, double probability) {
        BoardLayout layout = new BoardLayoutEngine(catalog).layout(genome);
        if (!layout.assembled()) return genome;
        Set<PortRef> used = usedPorts(genome);
        List<PortRef> free = freePorts(genome, used);
        List<TileConnection> connections = new ArrayList<>(genome.connections());
        Collections.shuffle(free, new Random(random.nextLong()));
        for (int i = 0; i < free.size(); i++) {
            PortRef first = free.get(i);
            if (used.contains(first)) continue;
            for (int j = i + 1; j < free.size(); j++) {
                PortRef second = free.get(j);
                if (first.tile.instanceId() == second.tile.instanceId() || used.contains(second)) continue;
                if (!aligned(first, second, layout)) continue;
                if (random.nextDouble() <= probability) {
                    connections.add(new TileConnection(first.tile.instanceId(), first.port,
                            second.tile.instanceId(), second.port));
                    used.add(first);
                    used.add(second);
                }
                break;
            }
        }
        return new BoardGenome(genome.tiles(), connections);
    }

    private Set<PortRef> usedPorts(BoardGenome genome) {
        Set<PortRef> used = new HashSet<>();
        Map<Integer, PlacedTile> tiles = index(genome);
        genome.connections().forEach(edge -> {
            used.add(new PortRef(tiles.get(edge.firstTile()), edge.firstPort()));
            used.add(new PortRef(tiles.get(edge.secondTile()), edge.secondPort()));
        });
        return used;
    }

    private List<PortRef> freePorts(BoardGenome genome, Set<PortRef> used) {
        List<PortRef> free = new ArrayList<>();
        for (PlacedTile tile : genome.tiles()) {
            catalog.require(tile.tileId()).rotate(tile.quarterTurns()).ports().stream()
                    .map(port -> new PortRef(tile, port.index()))
                    .filter(port -> !used.contains(port)).forEach(free::add);
        }
        return free;
    }

    private boolean aligned(PortRef first, PortRef second, BoardLayout layout) {
        return PortGeometry.aligned(first.tile, layout.origins().get(first.tile.instanceId()), first.port,
                second.tile, layout.origins().get(second.tile.instanceId()), second.port, catalog);
    }

    private Map<Integer, PlacedTile> index(BoardGenome genome) {
        Map<Integer, PlacedTile> result = new HashMap<>();
        genome.tiles().forEach(tile -> result.put(tile.instanceId(), tile));
        return result;
    }
}
