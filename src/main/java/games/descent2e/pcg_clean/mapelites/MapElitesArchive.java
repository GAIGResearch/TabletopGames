package games.descent2e.pcg_clean.mapelites;

import games.descent2e.pcg_clean.spatial.SpatialCandidate;

import java.util.Map;
import java.util.TreeMap;

/** Thread-safe archive; an elite minimises violations, then maximises quality. */
public final class MapElitesArchive {
    private final BehaviorDescriptor descriptor;
    private final Map<EliteCell, SpatialCandidate> elites = new TreeMap<>();
    private long evaluations;

    public MapElitesArchive(BehaviorDescriptor descriptor) { this.descriptor = descriptor; }

    public synchronized boolean offer(SpatialCandidate candidate) {
        evaluations++;
        EliteCell cell = descriptor.describe(candidate);
        SpatialCandidate incumbent = elites.get(cell);
        if (incumbent == null || better(candidate, incumbent)) {
            elites.put(cell, candidate);
            return true;
        }
        return false;
    }

    public synchronized MapElitesSnapshot snapshot() { return new MapElitesSnapshot(evaluations, elites); }

    private boolean better(SpatialCandidate candidate, SpatialCandidate incumbent) {
        int violations = Integer.compare(candidate.evaluation().violationCount(), incumbent.evaluation().violationCount());
        if (violations != 0) return violations < 0;
        int quality = -Double.compare(candidate.evaluation().quality().fitness(), incumbent.evaluation().quality().fitness());
        return quality != 0 ? quality < 0 : candidate.id() < incumbent.id();
    }
}
