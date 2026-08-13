package games.descent2e.pcg_clean.evaluation;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.BoardGenome;
import games.descent2e.pcg_clean.layout.BoardLayout;

public record EvaluationContext(BoardGenome genome, BoardLayout layout, TileCatalog catalog) {}
