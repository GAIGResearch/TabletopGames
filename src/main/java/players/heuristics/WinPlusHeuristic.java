package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

public class WinPlusHeuristic implements IStateHeuristic {

    double scale;
    public WinPlusHeuristic(double scale) {
        this.scale = scale;
    }
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.isNotTerminalForPlayer(playerId))
            return Math.min(gs.getHeuristicScore(playerId) / scale, 0.9);

        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.DRAW_GAME)
            return 0.5;
        return gs.getPlayerResults()[playerId].value;
    }
    @Override
    public double minValue() {
        return 0.0;
    }
    @Override
    public double maxValue() {
        return 1.0;
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