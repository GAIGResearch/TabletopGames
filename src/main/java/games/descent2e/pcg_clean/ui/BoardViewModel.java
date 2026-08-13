package games.descent2e.pcg_clean.ui;

import games.descent2e.pcg_clean.data.TileCatalog;
import games.descent2e.pcg_clean.domain.*;
import games.descent2e.pcg_clean.evolution.Candidate;
import games.descent2e.pcg_clean.layout.BoardLayout;
import games.descent2e.pcg_clean.layout.BoardLayoutEngine;
import games.descent2e.pcg_clean.spatial.SpatialCandidate;

import java.util.List;

/** Immutable data prepared for display; the Swing components do not run the EA. */
public record BoardViewModel(long candidateId, BoardGenome genome, BoardLayout layout,
                             TileCatalog catalog, boolean feasible, double fitness,
                             List<CriterionScore> criteria, List<String> violations) {
    public record CriterionScore(String name, double score, double weight) {
        public double weightedContribution() { return score * weight; }
    }

    public BoardViewModel {
        criteria = List.copyOf(criteria);
        violations = List.copyOf(violations);
    }

    public static BoardViewModel from(Candidate candidate, TileCatalog catalog) {
        BoardLayout layout = new BoardLayoutEngine(catalog).layout(candidate.genome());
        List<String> violations = candidate.evaluation().constraints().stream()
                .filter(result -> !result.satisfied())
                .flatMap(result -> result.violations().stream().map(v -> result.constraint() + ": " + v))
                .toList();
        List<CriterionScore> criteria = candidate.evaluation().criteria().stream()
                .map(score -> new CriterionScore(score.criterion(), score.score(), score.weight()))
                .toList();
        return new BoardViewModel(candidate.id(), candidate.genome(), layout, catalog,
                candidate.evaluation().feasible(), candidate.evaluation().fitness(), criteria, violations);
    }

    public static BoardViewModel from(SpatialCandidate candidate, TileCatalog catalog) {
        List<CriterionScore> criteria = candidate.evaluation().quality().criteria().stream()
                .map(score -> new CriterionScore(score.criterion(), score.score(), score.weight())).toList();
        return new BoardViewModel(candidate.id(), candidate.evaluation().phenotype().genome(),
                candidate.evaluation().phenotype().layout(), catalog,
                candidate.evaluation().violationCount() == 0,
                candidate.evaluation().quality().fitness(), criteria,
                candidate.evaluation().phenotype().violations());
    }

    public RotatedTile tile(PlacedTile placement) {
        return catalog.require(placement.tileId()).rotate(placement.quarterTurns());
    }

    public String summary() {
        return "Candidate %d — %s — fitness %.6f — %d pieces — %d traversable cells".formatted(
                candidateId, feasible ? "feasible" : "infeasible", fitness,
                genome.tiles().size(), layout.traversableCellCount());
    }
}
