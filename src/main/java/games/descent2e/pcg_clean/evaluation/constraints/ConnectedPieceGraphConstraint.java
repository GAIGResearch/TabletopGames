package games.descent2e.pcg_clean.evaluation.constraints;

import games.descent2e.pcg_clean.evaluation.*;

import java.util.HashSet;
import java.util.Set;

public final class ConnectedPieceGraphConstraint implements Constraint {
    public String name() { return "connected-piece-graph"; }

    public ConstraintResult evaluate(EvaluationContext context) {
        int start = context.genome().tiles().get(0).instanceId();
        Set<Integer> reached = new HashSet<>();
        visit(start, context, reached);
        return reached.size() == context.genome().tiles().size()
                ? ConstraintResult.satisfied(name())
                : ConstraintResult.violated(name(), "Only " + reached.size() + " of " + context.genome().tiles().size() + " pieces are connected");
    }

    private void visit(int node, EvaluationContext context, Set<Integer> reached) {
        if (!reached.add(node)) return;
        context.layout().graph().getOrDefault(node, Set.of()).forEach(next -> visit(next, context, reached));
    }
}
