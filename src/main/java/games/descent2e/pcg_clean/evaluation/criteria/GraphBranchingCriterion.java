package games.descent2e.pcg_clean.evaluation.criteria;

import games.descent2e.pcg_clean.evaluation.*;

/** Rewards a requested proportion of junction pieces (degree >= 3). */
public record GraphBranchingCriterion(double targetRatio, double weight) implements FitnessCriterion {
    public String name() { return "graph-branching"; }
    public double score(EvaluationContext context) {
        long junctions = context.layout().graph().values().stream().filter(n -> n.size() >= 3).count();
        double ratio = junctions / (double) context.genome().tiles().size();
        return 1.0 - Math.min(1.0, Math.abs(ratio - targetRatio));
    }
}
