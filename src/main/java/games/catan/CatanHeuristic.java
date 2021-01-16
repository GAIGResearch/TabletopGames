package games.catan;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class CatanHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CatanGameState cgs = (CatanGameState)gs;
        return cgs.getScore(playerId);
    }
}
