package games.descent2e.pcg_clean.evolution;

import games.descent2e.pcg_clean.domain.BoardGenome;

import java.util.random.RandomGenerator;

public interface MutationOperator {
    BoardGenome mutate(BoardGenome parent, RandomGenerator random);
}
