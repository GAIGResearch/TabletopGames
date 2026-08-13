package games.descent2e.pcg_clean.evaluation;

/** A soft objective normalised to [0,1], where one is best. */
public interface FitnessCriterion {
    String name();
    double weight();
    double score(EvaluationContext context);
}
