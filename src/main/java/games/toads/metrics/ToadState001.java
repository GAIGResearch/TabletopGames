package games.toads.metrics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class ToadState001 implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return 0;
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return 10.0;
    }
}
