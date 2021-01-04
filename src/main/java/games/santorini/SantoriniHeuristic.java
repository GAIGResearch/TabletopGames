package games.santorini;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class SantoriniHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        Utils.GameResult gameStatus = gs.getGameStatus();

        if(gameStatus == Utils.GameResult.LOSE)
            return -1;
        if(gameStatus == Utils.GameResult.WIN)
            return 1;

        return 0;
    }
}
