package games.seasaltpaper.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import games.seasaltpaper.SeaSaltPaperParameters;
import utilities.Utils;

public class ScoreHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME) {
            return maxValue();
        }
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME) {
            return minValue();
        }
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) gs.getGameParameters();

        // only return 1 if won
        return Utils.clamp(gs.getGameScore(playerId)/params.victoryCondition[gs.getNPlayers()-2], -1, 0.99);
    }
}
