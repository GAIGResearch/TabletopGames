package games.descent2e.pcg_clean.evaluation.constraints;

import games.descent2e.pcg_clean.evaluation.*;

public final class RequiredTileRoleConstraint implements Constraint {
    private final String role;
    private final int required;

    public RequiredTileRoleConstraint(String role, int required) {
        this.role = role.toLowerCase();
        this.required = required;
    }

    public String name() { return "exactly-" + required + "-" + role; }
    public ConstraintResult evaluate(EvaluationContext context) {
        long count = context.genome().tiles().stream().filter(t -> t.tileId().toLowerCase().startsWith(role)).count();
        return count == required ? ConstraintResult.satisfied(name())
                : ConstraintResult.violated(name(), "Expected " + required + " but found " + count);
    }
}
