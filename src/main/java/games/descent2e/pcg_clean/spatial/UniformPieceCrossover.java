package games.descent2e.pcg_clean.spatial;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

public final class UniformPieceCrossover {
    public SpatialChromosome cross(SpatialChromosome first, SpatialChromosome second, RandomGenerator random) {
        if (first.genes().size() != second.genes().size())
            throw new IllegalArgumentException("Chromosome lengths differ");
        List<PieceGene> genes = new ArrayList<>(first.genes().size());
        for (int i = 0; i < first.genes().size(); i++)
            genes.add(random.nextBoolean() ? first.genes().get(i) : second.genes().get(i));
        ensureSelected(genes, random);
        return new SpatialChromosome(genes);
    }

    private void ensureSelected(List<PieceGene> genes, RandomGenerator random) {
        if (genes.stream().noneMatch(PieceGene::selected)) {
            int index = random.nextInt(genes.size());
            genes.set(index, genes.get(index).withSelected(true));
        }
    }
}
