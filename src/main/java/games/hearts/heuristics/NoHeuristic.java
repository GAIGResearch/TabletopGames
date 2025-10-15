package games.hearts.heuristics;

import core.AbstractGameState;

public class NoHeuristic extends HeartsHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return 0.0;
    }
}
