package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class ScoreHeuristic implements IStateHeuristic {

    /**
     * This ignores the Win/Lose situation and only cares about the raw game score
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId  - player id
     * @return
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return gs.getGameScore(playerId);
    }
}
