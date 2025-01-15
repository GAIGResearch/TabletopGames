package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class OrdinalPosition implements IStateHeuristic {

    int playerCount = -1;
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // we score one point for each other player that we are beating (or equalling)
        if (playerCount == -1) playerCount = gs.getNPlayers();
        return gs.getNPlayers() - gs.getOrdinalPosition(playerId);
    }

    @Override
    public double minValue() {
        return 0.0;
    }
    @Override
    public double maxValue() {
        return playerCount - 1;
    }
}
