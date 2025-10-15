package games.hearts.heuristics;

import core.AbstractGameState;
import games.hearts.HeartsGameState;

public class ScoreFactorHeuristic extends HeartsHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        HeartsGameState tgs = (HeartsGameState) gs;
        double scoreFactor = (maxPossibleScore - tgs.getPlayerPoints(playerId) / maxPossibleScore);
        return scoreFactor;
    }
}

