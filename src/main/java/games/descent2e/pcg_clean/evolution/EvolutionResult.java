package games.descent2e.pcg_clean.evolution;

import java.util.List;

public record EvolutionResult(Candidate best, List<Candidate> finalPopulation,
                              List<Candidate> viewingArchive, long evaluations) {
    public EvolutionResult {
        finalPopulation = List.copyOf(finalPopulation);
        viewingArchive = List.copyOf(viewingArchive);
    }
}
