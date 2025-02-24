package games.seasaltpaper.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;
import games.seasaltpaper.cards.HandManager;
import utilities.Utils;

public class ScoreAndHandHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME) {
            return maxValue();
        }
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME) {
            return minValue();
        }
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) gs.getGameParameters();
        SeaSaltPaperGameState sspgs = (SeaSaltPaperGameState) gs;

//        double scoreAndHand = sspgs.getGameScore(playerId) + HandManager.calculatePoint(sspgs, playerId);
        double scoreAndHand = sspgs.getHeuristicScore(playerId);
        // only return 1 if won
        return Utils.clamp(scoreAndHand/params.victoryCondition[gs.getNPlayers()-2], -1, 0.99);
    }
}
