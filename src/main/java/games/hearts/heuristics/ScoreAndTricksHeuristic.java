package games.hearts.heuristics;

import core.AbstractGameState;
import games.hearts.HeartsGameState;

public class ScoreAndTricksHeuristic extends HeartsHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        HeartsGameState tgs = (HeartsGameState) gs;
        double scoreFactor = (maxPossibleScore - tgs.getPlayerPoints(playerId) / maxPossibleScore);

        double tricksTaken = tgs.playerTricksTaken[tgs.getCurrentPlayer()];
        double tricksFactor = (maxPossibleTricks - tricksTaken) / maxPossibleTricks;

        return scoreFactor + tricksFactor;
    }
}

