package games.descent2e.pcg_clean.spatial;

import java.util.List;

/** Fixed-length genotype: gene i always describes physical piece i. */
public record SpatialChromosome(List<PieceGene> genes) {
    public SpatialChromosome { genes = List.copyOf(genes); }
}
