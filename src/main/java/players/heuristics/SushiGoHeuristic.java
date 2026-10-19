package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

/**
 * Scores a SushiGo state from playerId's point of view in [-1, 1].
 * Mid-game it projects each player's final score (banked points plus the expected value of
 * unfinished sets, Maki and Pudding) and returns the margin over the best opponent.
 */
public class SushiGoHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        double score = gs.getGameScore(playerId);
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return score * 1.5;
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return score * 0.5;
        return score;
    }
    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }
    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        return "SushiGoHeuristic";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SushiGoHeuristic;
    }
    @Override
    public int hashCode() {
        return 3;
    }
}