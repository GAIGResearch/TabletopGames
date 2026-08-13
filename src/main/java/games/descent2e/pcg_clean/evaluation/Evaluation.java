package games.descent2e.pcg_clean.evaluation;

import java.util.List;

public record Evaluation(boolean feasible, double fitness, List<ConstraintResult> constraints,
                         List<FitnessBreakdown> criteria) {
    public Evaluation {
        constraints = List.copyOf(constraints);
        criteria = List.copyOf(criteria);
    }
}
