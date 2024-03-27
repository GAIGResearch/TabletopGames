package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class OrdinalPositionScaled implements IStateHeuristic {

    int playerCount = -1;
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // we score one point for each other player that we are beating (or equalling)
        if (playerCount == -1) playerCount = gs.getNPlayers();
        return (playerCount - gs.getOrdinalPosition(playerId)) / (playerCount - 1.0);
    }

    @Override
    public double minValue() {
        return 0.0;
    }
    @Override
    public double maxValue() {
        return 1.0;
    }
}
