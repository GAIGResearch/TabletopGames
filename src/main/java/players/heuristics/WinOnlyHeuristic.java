package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

public class WinOnlyHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.DRAW_GAME)
                return 0.5;
        return  gs.getPlayerResults()[playerId].value;
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
    public String toString() {
        return "WinOnlyHeuristic";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WinOnlyHeuristic;
    }
    @Override
    public int hashCode() {
        return 4;
    }
}
