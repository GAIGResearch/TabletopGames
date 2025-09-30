package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;

public class WinOnlyHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // We do not use the RESULT.value because this varies from -1 to +1
        return switch (gs.getPlayerResults()[playerId]) {
            case DRAW_GAME, TIMEOUT, GAME_ONGOING -> 0.5;
            case WIN_GAME -> 1.0;
            case LOSE_GAME, GAME_END, DISQUALIFY -> 0.0;
        };
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
