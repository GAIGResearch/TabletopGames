package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class PureScoreHeuristic implements IStateHeuristic {

    /**
     * This cares only about the raw game score
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId - player id
     * @return
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return gs.getGameScore(playerId);
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }
    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        return "PureScoreHeuristic";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof PureScoreHeuristic;
    }
    @Override
    public int hashCode() {
        return 6;
    }
}
