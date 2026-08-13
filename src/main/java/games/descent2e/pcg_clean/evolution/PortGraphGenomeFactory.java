package games.descent2e.pcg_clean.evolution;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.*;

import java.util.*;
import java.util.random.RandomGenerator;

/** Builds a connected tree, reserving one physical entrance and exit tile. */
public final class PortGraphGenomeFactory implements GenomeFactory {
    private record FreePort(int tileId, int port) {}
    private record Bridge(TileDefinition definition, int turns, FreePort firstParent, int firstChildPort,
                          FreePort secondParent, int secondChildPort) {}
    private final TileCatalog catalog;
    private final LoopCloser loopCloser;

    public PortGraphGenomeFactory(TileCatalog catalog) {
        this.catalog = catalog;
        this.loopCloser = new LoopCloser(catalog);
    }

    public BoardGenome create(int tileCount, RandomGenerator random) {
        for (int attempt = 0; attempt < 100; attempt++) {
            BoardGenome genome = tryCreate(tileCount, random);
            if (genome != null) return loopCloser.closeAvailableLoops(genome, random, 0.75);
        }
        throw new IllegalStateException("Could not create a compatible port graph");
    }

    private BoardGenome tryCreate(int tileCount, RandomGenerator random) {
        List<TileDefinition> entrances = matching("entrance");
        List<TileDefinition> exits = matching("exit");
        List<TileDefinition> regular = catalog.all().stream().filter(t -> !isSpecial(t.id())).toList();
        List<PlacedTile> tiles = new ArrayList<>();
        List<TileConnection> edges = new ArrayList<>();
        List<FreePort> free = new ArrayList<>();
        Set<String> usedDefinitions = new HashSet<>();

        addRoot(pick(entrances, random), random, tiles, free, usedDefinitions);
        for (int id = 1; id < tileCount; id++) {
            List<TileDefinition> pool = id == tileCount - 1 ? exits : regular;
            boolean attached = id < tileCount - 1 && random.nextDouble() < 0.45
                    && attachBridge(id, pool, random, tiles, edges, free, usedDefinitions);
            if (!attached && !attach(id, pool, random, tiles, edges, free, usedDefinitions)) return null;
        }
        return new BoardGenome(tiles, edges);
    }

    private boolean attachBridge(int id, List<TileDefinition> pool, RandomGenerator random,
                                 List<PlacedTile> tiles, List<TileConnection> edges,
                                 List<FreePort> free, Set<String> used) {
        if (free.size() < 2) return false;
        List<Bridge> bridges = findBridges(id, pool, tiles, edges, free, used);
        if (bridges.isEmpty()) return false;
        applyBridge(id, bridges.get(random.nextInt(bridges.size())), tiles, edges, free, used);
        return true;
    }

    private List<Bridge> findBridges(int id, List<TileDefinition> pool, List<PlacedTile> tiles,
                                     List<TileConnection> edges, List<FreePort> free, Set<String> used) {
        List<Bridge> result = new ArrayList<>();
        for (FreePort parent : free) for (TileDefinition definition : pool) {
            if (!used.contains(definition.id()))
                result.addAll(findBridges(id, definition, parent, tiles, edges, free));
        }
        return result;
    }

    private List<Bridge> findBridges(int id, TileDefinition definition, FreePort firstParent,
                                     List<PlacedTile> tiles, List<TileConnection> edges,
                                     List<FreePort> free) {
        List<Bridge> result = new ArrayList<>();
        PlacedTile parent = tiles.get(firstParent.tileId());
        Port parentPort = catalog.require(parent.tileId()).rotate(parent.quarterTurns()).port(firstParent.port());
        for (int turns = 0; turns < 4; turns++) {
            RotatedTile child = definition.rotate(turns);
            for (Port firstChild : child.ports()) {
                if (compatible(parentPort, firstChild)) result.addAll(findSecondConnections(id, definition, turns,
                        firstParent, firstChild, child, tiles, edges, free));
            }
        }
        return result;
    }

    private List<Bridge> findSecondConnections(int id, TileDefinition definition, int turns,
                                                FreePort firstParent, Port firstChild, RotatedTile child,
                                                List<PlacedTile> tiles, List<TileConnection> edges,
                                                List<FreePort> free) {
        PlacedTile placement = new PlacedTile(id, definition.id(), turns);
        BoardGenome partial = withConnection(tiles, edges, placement, firstParent, firstChild.index());
        var layout = new games.descent2e.pcg_clean.layout.BoardLayoutEngine(catalog).layout(partial);
        if (!layout.assembled()) return List.of();
        List<Bridge> result = new ArrayList<>();
        for (FreePort secondParent : free) for (Port secondChild : child.ports()) {
            if (secondParent.equals(firstParent) || secondChild.index() == firstChild.index()) continue;
            PlacedTile other = tiles.get(secondParent.tileId());
            if (games.descent2e.pcg_clean.layout.PortGeometry.aligned(
                    placement, layout.origins().get(id), secondChild.index(), other,
                    layout.origins().get(other.instanceId()), secondParent.port(), catalog))
                result.add(new Bridge(definition, turns, firstParent, firstChild.index(),
                        secondParent, secondChild.index()));
        }
        return result;
    }

    private void applyBridge(int id, Bridge bridge, List<PlacedTile> tiles,
                             List<TileConnection> edges, List<FreePort> free, Set<String> used) {
        PlacedTile child = new PlacedTile(id, bridge.definition.id(), bridge.turns);
        tiles.add(child);
        used.add(bridge.definition.id());
        edges.add(new TileConnection(bridge.firstParent.tileId(), bridge.firstParent.port(), id, bridge.firstChildPort));
        edges.add(new TileConnection(bridge.secondParent.tileId(), bridge.secondParent.port(), id, bridge.secondChildPort));
        free.remove(bridge.firstParent);
        free.remove(bridge.secondParent);
        bridge.definition.rotate(bridge.turns).ports().stream()
                .filter(port -> port.index() != bridge.firstChildPort && port.index() != bridge.secondChildPort)
                .forEach(port -> free.add(new FreePort(id, port.index())));
    }

    private BoardGenome withConnection(List<PlacedTile> tiles, List<TileConnection> edges, PlacedTile child,
                                       FreePort parent, int childPort) {
        List<PlacedTile> withChild = new ArrayList<>(tiles);
        withChild.add(child);
        List<TileConnection> withEdge = new ArrayList<>(edges);
        withEdge.add(new TileConnection(parent.tileId(), parent.port(), child.instanceId(), childPort));
        return new BoardGenome(withChild, withEdge);
    }

    private void addRoot(TileDefinition definition, RandomGenerator random, List<PlacedTile> tiles,
                         List<FreePort> free, Set<String> used) {
        PlacedTile root = new PlacedTile(0, definition.id(), random.nextInt(4));
        tiles.add(root);
        used.add(definition.id());
        definition.rotate(root.quarterTurns()).ports().forEach(p -> free.add(new FreePort(0, p.index())));
    }

    private boolean attach(int id, List<TileDefinition> pool, RandomGenerator random, List<PlacedTile> tiles,
                           List<TileConnection> edges, List<FreePort> free, Set<String> used) {
        for (int attempt = 0; attempt < 200 && !free.isEmpty(); attempt++) {
            FreePort parentRef = free.get(random.nextInt(free.size()));
            PlacedTile parent = tiles.get(parentRef.tileId());
            Port parentPort = catalog.require(parent.tileId()).rotate(parent.quarterTurns()).port(parentRef.port());
            TileDefinition definition = pickUnused(pool, used, random);
            if (definition == null) return false;
            int turns = random.nextInt(4);
            RotatedTile rotated = definition.rotate(turns);
            List<Port> matches = rotated.ports().stream().filter(p -> compatible(parentPort, p)).toList();
            if (matches.isEmpty()) continue;
            Port childPort = pick(matches, random);
            PlacedTile child = new PlacedTile(id, definition.id(), turns);
            tiles.add(child);
            used.add(definition.id());
            edges.add(new TileConnection(parent.instanceId(), parentPort.index(), id, childPort.index()));
            free.remove(parentRef);
            rotated.ports().stream().filter(p -> p.index() != childPort.index())
                    .forEach(p -> free.add(new FreePort(id, p.index())));
            return true;
        }
        return false;
    }

    private boolean compatible(Port a, Port b) {
        return a.direction() == b.direction().opposite() && a.cells().size() == b.cells().size();
    }

    private List<TileDefinition> matching(String prefix) {
        return catalog.all().stream().filter(t -> t.id().toLowerCase().startsWith(prefix)).toList();
    }

    private boolean isSpecial(String id) {
        String lower = id.toLowerCase();
        return lower.startsWith("entrance") || lower.startsWith("exit") || lower.startsWith("endcap")
                || lower.startsWith("extender") || lower.startsWith("transition");
    }

    private TileDefinition pickUnused(List<TileDefinition> pool, Set<String> used, RandomGenerator random) {
        List<TileDefinition> available = pool.stream().filter(t -> !used.contains(t.id())).toList();
        return available.isEmpty() ? null : pick(available, random);
    }

    private <T> T pick(List<T> values, RandomGenerator random) { return values.get(random.nextInt(values.size())); }
}
