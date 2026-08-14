package games.descent2e.pcg_clean.mapelites;

import games.descent2e.pcg_clean.spatial.SpatialCandidate;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Immutable point-in-time view safe to hand from the EA thread to Swing. */
public record MapElitesSnapshot(long evaluations, Map<EliteCell, SpatialCandidate> elites) {
    public MapElitesSnapshot { elites = Collections.unmodifiableMap(new TreeMap<>(elites)); }
}
