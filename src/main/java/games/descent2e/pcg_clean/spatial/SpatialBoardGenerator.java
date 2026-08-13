package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.evaluation.BoardEvaluator;

/** Composition root for the absolute-coordinate, two-objective generator. */
public final class SpatialBoardGenerator {
    private final TileCatalog tiles;
    private final PhysicalPieceCatalog pieces;

    public SpatialBoardGenerator(TileCatalog tiles) {
        this.tiles = tiles;
        this.pieces = new PhysicalPieceCatalog(tiles);
    }

    public SpatialEvolutionResult generate(SpatialConfig config) {
        BoardEvaluator quality = DescentQualityModel.create(tiles);
        SpatialDecoder decoder = new SpatialDecoder(tiles, pieces, config.minCoordinate(), config.maxCoordinate(),
                config.initialSelectedPieces());
        SpatialEvaluator evaluator = new SpatialEvaluator(tiles, decoder, quality);
        return new SpatialEvolutionEngine(config, new SpatialInitializer(pieces, config),
                new UniformPieceCrossover(), new SpatialMutation(pieces, config),
                new SpatialRepairOperator(tiles, pieces, config, decoder), evaluator).run();
    }

    public PhysicalPieceCatalog pieces() { return pieces; }
}
