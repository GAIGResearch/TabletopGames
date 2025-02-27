package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class NullHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        return 0.0;
    }

    @Override
    public String toString() {
        return "NullHeuristic";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullHeuristic;
    }
    @Override
    public int hashCode() {
        return 7;
    }
}
