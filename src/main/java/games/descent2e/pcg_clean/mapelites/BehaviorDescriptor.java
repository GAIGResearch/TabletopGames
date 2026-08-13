package games.descent2e.pcg_clean.mapelites;

import games.descent2e.pcg_clean.spatial.SpatialCandidate;

/** Maps a candidate to a behavioral niche, independently of its fitness. */
public interface BehaviorDescriptor {
    EliteCell describe(SpatialCandidate candidate);
    int branchingBins();
    int cycleBins();
    String branchingLabel(int bin);
    String cycleLabel(int bin);
}
