package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;

public class SushiGoHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState  gameState = (SGGameState) gs;
        if (gameState.getCardChoices().get(0).equals(1)) {
            return 1000;
        };

        return 1;
    }
}
