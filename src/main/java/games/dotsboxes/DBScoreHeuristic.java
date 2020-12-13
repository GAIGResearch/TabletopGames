package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

/**
 * Simple heuristic equal to the score of cells - this is deliberately not in the [-1, +1] range
 */
public class DBScoreHeuristic implements IStateHeuristic {
    /**
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId
     * @return - value of given state.
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DBGameState state = (DBGameState) gs;
        return state.nCellsPerPlayer[playerId];
    }
}
