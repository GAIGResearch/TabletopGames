package games.descent2e.pcg_clean.spatial;

public record GenerationStatistics(int generation, int minimumViolations, double averageViolations,
                                   double bestFitness, double averageFitness) {}
