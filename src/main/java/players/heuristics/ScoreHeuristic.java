package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import core.interfaces.ITunableParameters;
import utilities.Utils;

public class ScoreHeuristic implements IStateHeuristic {

    /**
     * This cares mostly about the raw game score - but will treat winning as a 50% bonus
     * and losing as halving it
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId - player id
     * @return
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getPlayerResults()[playerId] == Utils.GameResult.WIN)
            return gs.getGameScore(playerId) * 1.5;
        if (gs.getPlayerResults()[playerId] == Utils.GameResult.LOSE)
            return gs.getGameScore(playerId) * 0.5;
        return gs.getGameScore(playerId);
    }
}
