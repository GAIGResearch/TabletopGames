package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

public class LeaderHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        double score = gs.getGameScore(playerId) - bestOtherTeamScore(gs, playerId);
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME || gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            score *= 1.5;
        return score;
    }

    /**
     * The best score of any player on a different team to playerId (in a non-team game, each player is their own team).
     * Returns Double.NEGATIVE_INFINITY if there are no such players.
     */
    public static double bestOtherTeamScore(AbstractGameState gs, int playerId) {
        double bestOtherScore = Double.NEGATIVE_INFINITY;
        int team = gs.getTeam(playerId);
        for (int p = 0; p < gs.getNPlayers(); p++) {
            if (gs.getTeam(p) != team) {
                double otherScore = gs.getGameScore(p);
                if (otherScore > bestOtherScore)
                    bestOtherScore = otherScore;
            }
        }
        return bestOtherScore;
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
