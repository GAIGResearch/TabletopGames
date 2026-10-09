package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class GameDefaultHeuristic implements IStateHeuristic {
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return gs.getHeuristicScore(playerId);
    }

    @Override
    public String toString() {
        return "GameDefaultHeuristic";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof GameDefaultHeuristic;
    }
    @Override
    public int hashCode() {
        return 9;
    }
}
