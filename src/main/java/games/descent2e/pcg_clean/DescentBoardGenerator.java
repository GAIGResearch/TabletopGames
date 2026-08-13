package games.descent2e.pcg_clean;

import games.descent2e.pcg_clean.data.*;
import games.descent2e.pcg_clean.domain.Cell;
import games.descent2e.pcg_clean.evaluation.*;
import games.descent2e.pcg_clean.evaluation.constraints.*;
import games.descent2e.pcg_clean.evaluation.criteria.*;
import games.descent2e.pcg_clean.evolution.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Composition root for the standalone generator. Framework adapters belong outside this package. */
public final class DescentBoardGenerator {
    private final TileCatalog catalog;

    public DescentBoardGenerator(TileCatalog catalog) { this.catalog = catalog; }

    public static DescentBoardGenerator fromJson(Path tilesJson) throws IOException {
        return new DescentBoardGenerator(new TileCatalogLoader().load(tilesJson));
    }

    public EvolutionResult generate(GeneratorConfig config) {
        List<Constraint> constraints = List.of(
                new SuccessfulLayoutConstraint(),
                new ConnectedPieceGraphConstraint(),
                new RequiredTileRoleConstraint("entrance", 1),
                new RequiredTileRoleConstraint("exit", 1),
                new BoardExtentConstraint(64, 64));
        List<FitnessCriterion> criteria = List.of(
                new TargetCellCountCriterion(166, 2.0),
                new CompactnessCriterion(1.0),
                new GraphBranchingCriterion(0.15, 1.0),
                new GraphCycleCriterion(1, 1.5),
                new EntranceExitDistanceCriterion(7, 1.5),
                new TerrainCompositionCriterion(java.util.Map.of(
                        Cell.WATER, 0.12,
                        Cell.PIT, 0.04,
                        Cell.BLOCK, 0.04), 1.5));
        BoardEvaluator evaluator = new BoardEvaluator(catalog, constraints, criteria);
        MutationOperator mutation = new LoopClosingMutation(
                new CompatibleTileMutation(catalog), new LoopCloser(catalog), 0.75);
        return new EvolutionEngine(config, new PortGraphGenomeFactory(catalog), mutation, evaluator).run();
    }

    public static void main(String[] args) throws IOException {
        Path tilePath = Path.of(args.length > 0 ? args[0] : "data/descent2e/tiles.json");
        GeneratorConfig config = new GeneratorConfig(50, 100, 10, 10, 4, 20260813L);
        EvolutionResult result = fromJson(tilePath).generate(config);
        Candidate best = result.best();
        System.out.printf("Evaluated %,d boards; best is %s with fitness %.4f and %d pieces%n",
                result.evaluations(), best.evaluation().feasible() ? "feasible" : "infeasible",
                best.evaluation().fitness(), best.genome().tiles().size());
        best.evaluation().constraints().stream().filter(c -> !c.satisfied())
                .forEach(c -> System.out.println(c.constraint() + ": " + c.violations()));
        best.genome().tiles().forEach(System.out::println);
    }
}
