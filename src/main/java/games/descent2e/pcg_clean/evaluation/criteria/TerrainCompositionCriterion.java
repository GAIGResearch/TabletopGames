package games.descent2e.pcg_clean.evaluation.criteria;

import games.descent2e.pcg_clean.domain.Cell;
import games.descent2e.pcg_clean.evaluation.EvaluationContext;
import games.descent2e.pcg_clean.evaluation.FitnessCriterion;

import java.util.EnumMap;
import java.util.Map;

/** Rewards closeness to desired terrain proportions; sensitive to different A/B tile faces. */
public final class TerrainCompositionCriterion implements FitnessCriterion {
    private final Map<Cell, Double> targets;
    private final double weight;

    public TerrainCompositionCriterion(Map<Cell, Double> targets, double weight) {
        EnumMap<Cell, Double> orderedTargets = new EnumMap<>(Cell.class);
        orderedTargets.putAll(targets);
        this.targets = java.util.Collections.unmodifiableMap(orderedTargets);
        this.weight = weight;
        double targetTotal = targets.values().stream().mapToDouble(Double::doubleValue).sum();
        if (targetTotal > 1.0 + 1e-9) throw new IllegalArgumentException("Terrain targets cannot total more than one");
    }

    @Override public String name() { return "terrain-composition"; }
    @Override public double weight() { return weight; }

    @Override
    public double score(EvaluationContext context) {
        EnumMap<Cell, Long> counts = new EnumMap<>(Cell.class);
        long total = 0;
        for (Cell cell : context.layout().cells().values()) {
            if (cell == Cell.VOID || cell == Cell.OPEN) continue;
            counts.merge(cell, 1L, Long::sum);
            total++;
        }
        if (total == 0) return 0;
        long observedTotal = total;
        double error = targets.entrySet().stream().mapToDouble(entry ->
                Math.abs(counts.getOrDefault(entry.getKey(), 0L) / (double) observedTotal - entry.getValue())).sum();
        return Math.max(0, 1.0 - error);
    }
}
