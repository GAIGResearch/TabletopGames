package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class GameDefaultHeuristic implements IStateHeuristic {

    /**
     * This cares only about the raw game score
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId - player id
     * @return
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return gs.getHeuristicScore(playerId);
    }

    @Override
    public double minValue() {
        return -1.0;
    }
    @Override
    public double maxValue() {
        return 1.0;
    }

}
