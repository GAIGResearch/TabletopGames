package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class OrdinalPosition implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // we score one point for each other player that we are beating (or equalling)
        return gs.getNPlayers() - gs.getOrdinalPosition(playerId);
    }
}
