package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

public class WinPlusHeuristic extends WinOnlyHeuristic {

    double scale;
    public WinPlusHeuristic(double scale) {
        this.scale = scale;
    }
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.isNotTerminalForPlayer(playerId))
            return Math.max(0.05, Math.min(gs.getHeuristicScore(playerId) / scale, 0.95));

        return super.evaluateState(gs, playerId);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WinPlusHeuristic;
    }
    @Override
    public int hashCode() {
        return 5;
    }
}