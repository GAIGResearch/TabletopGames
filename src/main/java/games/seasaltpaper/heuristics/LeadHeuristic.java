package games.seasaltpaper.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SeaSaltPaperParameters;
import games.seasaltpaper.cards.HandManager;
import utilities.Utils;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

public class LeadHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME) {
            return maxValue();
        }
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME) {
            return minValue();
        }
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) gs.getGameParameters();
        double[] scores = new double[gs.getNPlayers()];
        double max = 0; // max score of every other players
        for (int i=0; i < gs.getNPlayers(); i++) {
            scores[i] = gs.getHeuristicScore(i);
//            scores[i] = HandManager.calculatePoint((SeaSaltPaperGameState) gs, i);
            if (i != playerId) {
                if (scores[i] > max) {
                    max = scores[i];
                }
            }
        }
        // only return 1 if won
        return Utils.clamp((scores[playerId] - max) / params.victoryCondition[gs.getNPlayers() - 2], -1, 1);
    }
}
