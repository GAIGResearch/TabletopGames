package games.descent2e.pcg_clean.evaluation.criteria;

import games.descent2e.pcg_clean.evaluation.EvaluationContext;
import games.descent2e.pcg_clean.evaluation.FitnessCriterion;

/** Scores the cyclomatic number E - V + C against a requested loop count. */
public record GraphCycleCriterion(int targetCycles, double weight) implements FitnessCriterion {
    public GraphCycleCriterion {
        if (targetCycles < 0) throw new IllegalArgumentException("Target cycles cannot be negative");
    }

    @Override public String name() { return "piece-graph-cycles"; }

    @Override
    public double score(EvaluationContext context) {
        int vertices = context.genome().tiles().size();
        int edges = context.genome().connections().size();
        int components = components(context);
        int cycles = Math.max(0, edges - vertices + components);
        if (targetCycles == 0) return cycles == 0 ? 1 : 1.0 / (1.0 + cycles);
        return 1.0 / (1.0 + Math.abs(cycles - targetCycles) / (double) targetCycles);
    }

    private int components(EvaluationContext context) {
        java.util.Set<Integer> remaining = new java.util.HashSet<>(context.layout().graph().keySet());
        int components = 0;
        while (!remaining.isEmpty()) {
            components++;
            visit(remaining.iterator().next(), context, remaining);
        }
        return components;
    }

    private void visit(int node, EvaluationContext context, java.util.Set<Integer> remaining) {
        if (!remaining.remove(node)) return;
        context.layout().graph().getOrDefault(node, java.util.Set.of())
                .forEach(next -> visit(next, context, remaining));
    }
}
