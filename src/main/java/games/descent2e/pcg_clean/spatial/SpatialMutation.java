package games.descent2e.pcg_clean.spatial;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Per-piece mutation with geometrically distributed coordinate steps. */
public final class SpatialMutation {
    private final PhysicalPieceCatalog pieces;
    private final SpatialConfig config;

    public SpatialMutation(PhysicalPieceCatalog pieces, SpatialConfig config) {
        this.pieces = pieces;
        this.config = config;
    }

    public SpatialChromosome mutate(SpatialChromosome source, RandomGenerator random) {
        List<PieceGene> genes = new ArrayList<>(source.genes());
        boolean changed = false;
        for (int i = 0; i < genes.size(); i++) {
            if (random.nextDouble() < config.geneMutationRate()) {
                genes.set(i, mutateGene(i, genes.get(i), random));
                changed = true;
            }
        }
        if (!changed) {
            int index = random.nextInt(genes.size());
            genes.set(index, mutateGene(index, genes.get(index), random));
        }
        if (genes.stream().noneMatch(PieceGene::selected)) {
            int index = random.nextInt(genes.size());
            genes.set(index, genes.get(index).withSelected(true));
        }
        return new SpatialChromosome(genes);
    }

    private PieceGene mutateGene(int index, PieceGene gene, RandomGenerator random) {
        return switch (random.nextInt(4)) {
            case 0 -> gene.withSelected(!gene.selected());
            case 1 -> mutateFace(index, gene, random);
            case 2 -> gene.withRotation(gene.quarterTurns() + (random.nextBoolean() ? 1 : -1));
            case 3 -> move(gene, random);
            default -> throw new IllegalStateException();
        };
    }

    private PieceGene mutateFace(int index, PieceGene gene, RandomGenerator random) {
        int faces = pieces.pieces().get(index).faces().size();
        return faces == 1 ? gene.withRotation(gene.quarterTurns() + 1)
                : gene.withFace((gene.face() + 1 + random.nextInt(faces - 1)) % faces);
    }

    private PieceGene move(PieceGene gene, RandomGenerator random) {
        int step = 1;
        while (step < coordinateRange() && random.nextDouble() < 0.35) step++;
        int dx = 0;
        int dy = 0;
        if (random.nextBoolean()) dx = random.nextBoolean() ? step : -step;
        else dy = random.nextBoolean() ? step : -step;
        return gene.withPosition(clamp(gene.x() + dx), clamp(gene.y() + dy));
    }

    private int coordinateRange() { return config.maxCoordinate() - config.minCoordinate(); }
    private int clamp(int value) { return Math.max(config.minCoordinate(), Math.min(config.maxCoordinate(), value)); }
}
