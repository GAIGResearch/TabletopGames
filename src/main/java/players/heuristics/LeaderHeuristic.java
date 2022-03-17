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

        if (gs.getPlayerResults()[playerId] == Utils.GameResult.WIN)
            return gs.getGameScore(playerId) * 2.0 - bestOtherScore;
        return score - bestOtherScore;
    }
}
