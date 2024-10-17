package games.hearts.heuristics;

import core.AbstractGameState;
import games.hearts.HeartsGameState;

public class TricksAndHighCardsHeuristic extends HeartsHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        HeartsGameState tgs = (HeartsGameState) gs;

        double tricksTaken = tgs.playerTricksTaken[tgs.getCurrentPlayer()];
        double tricksFactor = (maxPossibleTricks - tricksTaken) / maxPossibleTricks;

        double highValueCardFactor = 0.0;
        int highValueCards = (int) tgs.getPlayerDecks().get(playerId).getComponents().stream()
                .filter(card -> (card).number > HIGH_VALUE_THRESHOLD)
                .count();
        if(highValueCards < maxHighValueCards) {
            highValueCardFactor = (maxHighValueCards - highValueCards) / maxHighValueCards * MAX_HIGH_VALUE_CARD_PASS_BONUS;
        }

        return tricksFactor + highValueCardFactor;
    }
}

