package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class WinOnlyHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getPlayerResults()[playerId] == Utils.GameResult.DRAW)
                return 0.5;
        return  gs.getPlayerResults()[playerId].value;
    }
}
