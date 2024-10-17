package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class NullHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        return 0.0;
    }
}
