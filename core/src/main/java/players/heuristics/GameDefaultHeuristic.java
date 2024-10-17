package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class GameDefaultHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return gs.getHeuristicScore(playerId);
    }
}
