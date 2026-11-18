package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

/**
 * As {@link WinOnlyHeuristic}, but a draw scores as a loss (0.0) rather than 0.5.
 */
public class DrawAsLossHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        // We do not use the RESULT.value because this varies from -1 to +1
        return switch (gs.getPlayerResults()[playerId]) {
            case DRAW_GAME, LOSE_GAME, GAME_END, DISQUALIFY -> 0.0;
            case TIMEOUT, GAME_ONGOING -> 0.5;
            case WIN_GAME -> 1.0;
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
        return "DrawAsLossHeuristic";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DrawAsLossHeuristic;
    }

    @Override
    public int hashCode() {
        return 11;
    }
}
