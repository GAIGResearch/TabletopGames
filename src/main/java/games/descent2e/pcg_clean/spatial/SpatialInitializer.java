package games.descent2e.pcg_clean.spatial;

import java.util.*;
import java.util.random.RandomGenerator;

public final class SpatialInitializer {
    private final PhysicalPieceCatalog pieces;
    private final SpatialConfig config;

    public SpatialInitializer(PhysicalPieceCatalog pieces, SpatialConfig config) {
        this.pieces = pieces;
        this.config = config;
    }

    public SpatialChromosome create(RandomGenerator random) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < pieces.pieces().size(); i++) indices.add(i);
        Collections.shuffle(indices, new Random(random.nextLong()));
        Set<Integer> selected = new HashSet<>(indices.subList(0,
                Math.min(config.initialSelectedPieces(), indices.size())));
        selectRole(selected, "entrance");
        selectRole(selected, "exit");
        List<PieceGene> genes = new ArrayList<>(pieces.pieces().size());
        for (int i = 0; i < pieces.pieces().size(); i++) {
            PhysicalPiece piece = pieces.pieces().get(i);
            genes.add(new PieceGene(selected.contains(i), random.nextInt(piece.faces().size()),
                    coordinate(random), coordinate(random), random.nextInt(4)));
        }
        return new SpatialChromosome(genes);
    }

    private void selectRole(Set<Integer> selected, String role) {
        for (int i = 0; i < pieces.pieces().size(); i++) {
            if (pieces.pieces().get(i).id().startsWith(role)) {
                selected.add(i);
                return;
            }
        }
    }

    private int coordinate(RandomGenerator random) {
        // Triangular distribution: still covers the full range but starts with more interacting pieces.
        int first = random.nextInt(config.minCoordinate(), config.maxCoordinate() + 1);
        int second = random.nextInt(config.minCoordinate(), config.maxCoordinate() + 1);
        return Math.round((first + second) / 2.0f);
    }
}
