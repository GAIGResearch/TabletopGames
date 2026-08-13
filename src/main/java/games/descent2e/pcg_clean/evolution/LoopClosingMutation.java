package games.descent2e.pcg_clean.evolution;

import games.descent2e.pcg_clean.domain.BoardGenome;

import java.util.random.RandomGenerator;

/** Decorates any mutation operator with geometry-aware loop closure. */
public final class LoopClosingMutation implements MutationOperator {
    private final MutationOperator delegate;
    private final LoopCloser loopCloser;
    private final double closureProbability;

    public LoopClosingMutation(MutationOperator delegate, LoopCloser loopCloser, double closureProbability) {
        this.delegate = delegate;
        this.loopCloser = loopCloser;
        this.closureProbability = closureProbability;
    }

    @Override
    public BoardGenome mutate(BoardGenome parent, RandomGenerator random) {
        return loopCloser.closeAvailableLoops(delegate.mutate(parent, random), random, closureProbability);
    }
}
