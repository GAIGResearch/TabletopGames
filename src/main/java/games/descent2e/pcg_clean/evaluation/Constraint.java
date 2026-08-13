package games.descent2e.pcg_clean.evaluation;

/** A hard requirement. Constraints report violations rather than hiding them in fitness arithmetic. */
public interface Constraint {
    String name();
    ConstraintResult evaluate(EvaluationContext context);
}
