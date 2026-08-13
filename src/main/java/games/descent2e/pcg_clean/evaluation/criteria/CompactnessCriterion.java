package games.descent2e.pcg_clean.evaluation.criteria;

import games.descent2e.pcg_clean.domain.GridPoint;
import games.descent2e.pcg_clean.evaluation.*;

public record CompactnessCriterion(double weight) implements FitnessCriterion {
    public String name() { return "compactness"; }
    public double score(EvaluationContext context) {
        if (context.layout().cells().isEmpty()) return 0;
        int minX = context.layout().cells().keySet().stream().mapToInt(GridPoint::x).min().orElse(0);
        int maxX = context.layout().cells().keySet().stream().mapToInt(GridPoint::x).max().orElse(0);
        int minY = context.layout().cells().keySet().stream().mapToInt(GridPoint::y).min().orElse(0);
        int maxY = context.layout().cells().keySet().stream().mapToInt(GridPoint::y).max().orElse(0);
        long area = (long) (maxX - minX + 1) * (maxY - minY + 1);
        return context.layout().traversableCellCount() / (double) area;
    }
}
