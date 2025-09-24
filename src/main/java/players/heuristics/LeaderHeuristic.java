package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

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
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME || gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            score *= 1.5;
        return score;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LeaderHeuristic;
    }
    @Override
    public int hashCode() {
        return 8;
    }
    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }
    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }
}
