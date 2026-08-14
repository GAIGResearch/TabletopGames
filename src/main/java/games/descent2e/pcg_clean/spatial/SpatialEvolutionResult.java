package games.descent2e.pcg_clean.spatial;

import java.util.List;

public record SpatialEvolutionResult(List<SpatialCandidate> paretoFront, List<SpatialCandidate> finalPopulation,
                                     List<SpatialCandidate> viewingArchive, List<GenerationStatistics> history,
                                     long evaluations) {
    public SpatialEvolutionResult {
        paretoFront = List.copyOf(paretoFront);
        finalPopulation = List.copyOf(finalPopulation);
        viewingArchive = List.copyOf(viewingArchive);
        history = List.copyOf(history);
    }
}
