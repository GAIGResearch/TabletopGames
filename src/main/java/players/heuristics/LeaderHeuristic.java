package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class LeaderHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        double score = gs.getGameScore(playerId);
        double bestOtherScore = Double.NEGATIVE_INFINITY;
        for (int p = 0; p < gs.getNPlayers(); p++) {
            if (p != playerId) {
                double otherScore = gs.getGameScore(p);
                if (otherScore > bestOtherScore)
                    bestOtherScore = otherScore;
            }
        }

        score = score - bestOtherScore;
        if (gs.getPlayerResults()[playerId] == Utils.GameResult.WIN || gs.getPlayerResults()[playerId] == Utils.GameResult.LOSE)
            score *= 1.5;
        return score;
    }
}
