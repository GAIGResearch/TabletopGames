package games.descent2e.pcg_clean.evolution;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.*;

import java.util.*;
import java.util.random.RandomGenerator;

/** Replaces one non-terminal piece while preserving every incident port contract. */
public final class CompatibleTileMutation implements MutationOperator {
    private final TileCatalog catalog;

    public CompatibleTileMutation(TileCatalog catalog) { this.catalog = catalog; }

    public BoardGenome mutate(BoardGenome parent, RandomGenerator random) {
        if (parent.tiles().size() <= 2) return parent;
        int selectedIndex = 1 + random.nextInt(parent.tiles().size() - 2);
        PlacedTile selected = parent.tiles().get(selectedIndex);
        List<TileConnection> incident = parent.connections().stream().filter(e -> e.contains(selected.instanceId())).toList();
        Set<String> used = parent.tiles().stream().map(PlacedTile::tileId).collect(java.util.stream.Collectors.toSet());
        List<PlacedTile> replacements = candidates(selected, incident, parent, used);
        if (replacements.isEmpty()) return parent;
        List<PlacedTile> tiles = new ArrayList<>(parent.tiles());
        tiles.set(selectedIndex, replacements.get(random.nextInt(replacements.size())));
        return new BoardGenome(tiles, parent.connections());
    }

    private List<PlacedTile> candidates(PlacedTile selected, List<TileConnection> incident,
                                        BoardGenome genome, Set<String> used) {
        List<PlacedTile> result = new ArrayList<>();
        for (TileDefinition definition : catalog.all()) {
            if (used.contains(definition.id()) || isSpecial(definition.id())) continue;
            for (int turns = 0; turns < 4; turns++) {
                PlacedTile replacement = new PlacedTile(selected.instanceId(), definition.id(), turns);
                if (incident.stream().allMatch(e -> preservesPort(e, selected.instanceId(), replacement, genome)))
                    result.add(replacement);
            }
        }
        return result;
    }

    private boolean preservesPort(TileConnection edge, int selectedId, PlacedTile replacement, BoardGenome genome) {
        int replacementPort = edge.firstTile() == selectedId ? edge.firstPort() : edge.secondPort();
        int otherPortIndex = edge.firstTile() == selectedId ? edge.secondPort() : edge.firstPort();
        PlacedTile other = genome.tiles().stream().filter(t -> t.instanceId() == edge.other(selectedId)).findFirst().orElseThrow();
        RotatedTile candidate = catalog.require(replacement.tileId()).rotate(replacement.quarterTurns());
        if (candidate.ports().stream().noneMatch(p -> p.index() == replacementPort)) return false;
        Port a = candidate.port(replacementPort);
        Port b = catalog.require(other.tileId()).rotate(other.quarterTurns()).port(otherPortIndex);
        return a.direction() == b.direction().opposite() && a.cells().size() == b.cells().size();
    }

    private boolean isSpecial(String id) {
        String lower = id.toLowerCase();
        return lower.startsWith("entrance") || lower.startsWith("exit") || lower.startsWith("endcap")
                || lower.startsWith("extender") || lower.startsWith("transition");
    }
}
