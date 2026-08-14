package games.descent2e.pcg_clean.evaluation;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.BoardGenome;
import games.descent2e.pcg_clean.layout.BoardLayoutEngine;

import java.util.List;

public final class BoardEvaluator {
    private final TileCatalog catalog;
    private final BoardLayoutEngine layoutEngine;
    private final List<Constraint> constraints;
    private final List<FitnessCriterion> criteria;

    public BoardEvaluator(TileCatalog catalog, List<Constraint> constraints, List<FitnessCriterion> criteria) {
        this.catalog = catalog;
        this.layoutEngine = new BoardLayoutEngine(catalog);
        this.constraints = List.copyOf(constraints);
        this.criteria = List.copyOf(criteria);
    }

    public Evaluation evaluate(BoardGenome genome) {
        return evaluate(new EvaluationContext(genome, layoutEngine.layout(genome), catalog));
    }

    public Evaluation evaluate(EvaluationContext context) {
        List<ConstraintResult> results = constraints.stream().map(c -> c.evaluate(context)).toList();
        List<FitnessBreakdown> scores = criteria.stream()
                .map(c -> new FitnessBreakdown(c.name(), clamp(c.score(context)), c.weight())).toList();
        double totalWeight = scores.stream().mapToDouble(FitnessBreakdown::weight).sum();
        double fitness = totalWeight == 0 ? 0 : scores.stream()
                .mapToDouble(s -> s.score() * s.weight()).sum() / totalWeight;
        return new Evaluation(results.stream().allMatch(ConstraintResult::satisfied), fitness, results, scores);
    }

    private double clamp(double value) { return Math.max(0, Math.min(1, value)); }
}
