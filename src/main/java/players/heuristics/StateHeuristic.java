
package players.heuristics;

import core.AbstractGameState;

public abstract class StateHeuristic {
    public abstract double evaluateState(AbstractGameState gs);
}