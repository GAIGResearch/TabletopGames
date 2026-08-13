package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.evaluation.*;

public final class SpatialEvaluator {
    private final TileCatalog catalog;
    private final SpatialDecoder decoder;
    private final BoardEvaluator qualityEvaluator;

    public SpatialEvaluator(TileCatalog catalog, SpatialDecoder decoder, BoardEvaluator qualityEvaluator) {
        this.catalog = catalog;
        this.decoder = decoder;
        this.qualityEvaluator = qualityEvaluator;
    }

    public SpatialEvaluation evaluate(SpatialChromosome chromosome) {
        SpatialPhenotype phenotype = decoder.decode(chromosome);
        Evaluation quality = qualityEvaluator.evaluate(new EvaluationContext(
                phenotype.genome(), phenotype.layout(), catalog));
        return new SpatialEvaluation(phenotype, phenotype.violations().size(), quality);
    }
}
