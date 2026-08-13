package games.descent2e.pcg_clean.evaluation;

import java.util.List;

public record ConstraintResult(String constraint, List<String> violations) {
    public ConstraintResult { violations = List.copyOf(violations); }
    public boolean satisfied() { return violations.isEmpty(); }
    public static ConstraintResult satisfied(String name) { return new ConstraintResult(name, List.of()); }
    public static ConstraintResult violated(String name, String problem) { return new ConstraintResult(name, List.of(problem)); }
}
