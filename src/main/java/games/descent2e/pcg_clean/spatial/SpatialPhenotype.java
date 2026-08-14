package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.domain.BoardGenome;
import games.descent2e.pcg_clean.layout.BoardLayout;

import java.util.List;

public record SpatialPhenotype(BoardGenome genome, BoardLayout layout, List<String> violations) {
    public SpatialPhenotype { violations = List.copyOf(violations); }
}
