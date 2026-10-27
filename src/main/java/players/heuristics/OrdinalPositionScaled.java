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

    // no settings (any fields are fixed, or worked out from the game): any two are the same heuristic, so that one
    // read back from JSON equals the one written
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }
}
