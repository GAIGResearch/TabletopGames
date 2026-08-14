package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.evaluation.BoardEvaluator;
import games.descent2e.pcg_clean.mapelites.*;

/** Composition root for the MAP-Elites variant of the spatial generator. */
public final class SpatialMapElitesGenerator {
    private final TileCatalog tiles;
    private final PhysicalPieceCatalog pieces;

    public SpatialMapElitesGenerator(TileCatalog tiles) {
        this.tiles = tiles;
        this.pieces = new PhysicalPieceCatalog(tiles);
    }

    public MapElitesResult generate(SpatialConfig config, BehaviorDescriptor descriptor,
                                    MapElitesListener listener) {
        BoardEvaluator quality = DescentQualityModel.create(tiles);
        SpatialDecoder decoder = new SpatialDecoder(tiles, pieces, config.minCoordinate(), config.maxCoordinate(),
                config.initialSelectedPieces());
        SpatialEvaluator evaluator = new SpatialEvaluator(tiles, decoder, quality);
        return new MapElitesEngine(config, new SpatialInitializer(pieces, config),
                new UniformPieceCrossover(), new SpatialMutation(pieces, config),
                new SpatialRepairOperator(tiles, pieces, config, decoder), evaluator,
                descriptor, listener).run();
    }
}
