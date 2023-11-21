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
}