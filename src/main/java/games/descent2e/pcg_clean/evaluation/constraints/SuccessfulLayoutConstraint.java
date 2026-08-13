package games.descent2e.pcg_clean.evaluation.constraints;

import games.descent2e.pcg_clean.evaluation.*;

public final class SuccessfulLayoutConstraint implements Constraint {
    public String name() { return "valid-layout"; }
    public ConstraintResult evaluate(EvaluationContext context) {
        return new ConstraintResult(name(), context.layout().problems());
    }
}
