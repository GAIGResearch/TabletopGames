package games.descent2e.pcg_clean.spatial;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.Cell;
import games.descent2e.pcg_clean.evaluation.BoardEvaluator;
import games.descent2e.pcg_clean.evaluation.FitnessCriterion;
import games.descent2e.pcg_clean.evaluation.criteria.*;

import java.util.List;
import java.util.Map;

/** One shared quality definition, independent of NSGA-II or MAP-Elites. */
final class DescentQualityModel {
    private DescentQualityModel() {}

    static BoardEvaluator create(TileCatalog tiles) {
        List<FitnessCriterion> criteria = List.of(
                new TargetCellCountCriterion(166, 2.0), new CompactnessCriterion(1.0),
                new GraphBranchingCriterion(0.15, 1.0), new GraphCycleCriterion(1, 1.5),
                new EntranceExitDistanceCriterion(7, 1.5),
                new TerrainCompositionCriterion(Map.of(
                        Cell.WATER, 0.12, Cell.PIT, 0.04, Cell.BLOCK, 0.04), 1.5));
        return new BoardEvaluator(tiles, List.of(), criteria);
    }
}
