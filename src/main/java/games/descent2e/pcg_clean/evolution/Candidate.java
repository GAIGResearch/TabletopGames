package games.descent2e.pcg_clean.evolution;

import games.descent2e.pcg_clean.domain.BoardGenome;
import games.descent2e.pcg_clean.evaluation.Evaluation;

public record Candidate(long id, BoardGenome genome, Evaluation evaluation) implements Comparable<Candidate> {
    @Override
    public int compareTo(Candidate other) {
        int feasibleFirst = Boolean.compare(other.evaluation.feasible(), evaluation.feasible());
        if (feasibleFirst != 0) return feasibleFirst;
        return -Double.compare(evaluation.fitness(), other.evaluation.fitness());
    }
}
