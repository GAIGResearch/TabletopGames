package games.descent2e.pcg_clean.evaluation.constraints;

import games.descent2e.pcg_clean.domain.GridPoint;
import games.descent2e.pcg_clean.evaluation.*;

public final class BoardExtentConstraint implements Constraint {
    private final int maxWidth;
    private final int maxHeight;

    public BoardExtentConstraint(int maxWidth, int maxHeight) { this.maxWidth = maxWidth; this.maxHeight = maxHeight; }
    public String name() { return "board-extent"; }
    public ConstraintResult evaluate(EvaluationContext context) {
        if (context.layout().cells().isEmpty()) return ConstraintResult.violated(name(), "Board has no cells");
        int minX = context.layout().cells().keySet().stream().mapToInt(GridPoint::x).min().orElse(0);
        int maxX = context.layout().cells().keySet().stream().mapToInt(GridPoint::x).max().orElse(0);
        int minY = context.layout().cells().keySet().stream().mapToInt(GridPoint::y).min().orElse(0);
        int maxY = context.layout().cells().keySet().stream().mapToInt(GridPoint::y).max().orElse(0);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        return width <= maxWidth && height <= maxHeight ? ConstraintResult.satisfied(name())
                : ConstraintResult.violated(name(), "Extent " + width + "x" + height + " exceeds " + maxWidth + "x" + maxHeight);
    }
}
