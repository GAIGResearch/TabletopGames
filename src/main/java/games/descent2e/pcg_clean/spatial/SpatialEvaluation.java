package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.evaluation.Evaluation;

public record SpatialEvaluation(SpatialPhenotype phenotype, int violationCount, Evaluation quality) {}
