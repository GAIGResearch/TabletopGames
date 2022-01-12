package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import core.interfaces.ITunableParameters;
import utilities.Utils;

import static utilities.Utils.GameResult.DRAW;
import static utilities.Utils.GameResult.WIN;

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
        double score = gs.getGameScore(playerId);
        if (gs.getPlayerResults()[playerId] == Utils.GameResult.WIN)
            return score * 1.5;
        if (gs.getPlayerResults()[playerId] == Utils.GameResult.LOSE)
            return score * 0.5;
        return score;
    }
}
