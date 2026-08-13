package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.TileDefinition;

import java.util.*;

/** Groups JSON A/B faces into the physical components available to a chromosome. */
public final class PhysicalPieceCatalog {
    private final List<PhysicalPiece> pieces;

    public PhysicalPieceCatalog(TileCatalog tiles) {
        Map<String, List<String>> grouped = new TreeMap<>();
        tiles.all().forEach(tile -> grouped.computeIfAbsent(physicalId(tile), ignored -> new ArrayList<>()).add(tile.id()));
        pieces = grouped.entrySet().stream()
                .map(entry -> new PhysicalPiece(entry.getKey(), entry.getValue().stream().sorted().toList()))
                .toList();
    }

    public List<PhysicalPiece> pieces() { return pieces; }

    private String physicalId(TileDefinition tile) {
        String id = tile.id();
        if (id.length() > 1) {
            char suffix = Character.toUpperCase(id.charAt(id.length() - 1));
            if (suffix == 'A' || suffix == 'B') return id.substring(0, id.length() - 1).toLowerCase(Locale.ROOT);
        }
        return id.toLowerCase(Locale.ROOT);
    }
}
