package games.descent2e.pcg_clean.evaluation.criteria;

import games.descent2e.pcg_clean.evaluation.*;

public record TargetCellCountCriterion(long target, double weight) implements FitnessCriterion {
    public String name() { return "target-traversable-cells"; }
    public double score(EvaluationContext context) {
        long actual = context.layout().traversableCellCount();
        return target == 0 ? 0 : 1.0 / (1.0 + Math.abs(actual - target) / (double) target);
    }
}
