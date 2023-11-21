package players.heuristics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;

public class NullActionHeuristic implements IActionHeuristic {
    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state) {
        return 0.0;
    }
}
