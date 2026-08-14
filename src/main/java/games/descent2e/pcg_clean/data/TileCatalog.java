package games.descent2e.pcg_clean.data;

import games.descent2e.pcg_clean.domain.TileDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TileCatalog {
    private final Map<String, TileDefinition> byId;
    private final List<TileDefinition> ordered;

    public TileCatalog(Collection<TileDefinition> definitions) {
        ordered = definitions.stream().sorted(java.util.Comparator.comparing(TileDefinition::id)).toList();
        Map<String, TileDefinition> index = new LinkedHashMap<>();
        ordered.forEach(tile -> {
            if (index.putIfAbsent(tile.id(), tile) != null)
                throw new IllegalArgumentException("Duplicate tile: " + tile.id());
        });
        byId = java.util.Collections.unmodifiableMap(index);
    }

    public TileDefinition require(String id) {
        TileDefinition definition = byId.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown tile: " + id);
        return definition;
    }

    /** Stable order is part of seeded-generation reproducibility. */
    public List<TileDefinition> all() { return ordered; }
}
