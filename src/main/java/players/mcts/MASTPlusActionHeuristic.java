package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;

public class MASTPlusActionHeuristic extends MASTActionHeuristic {

    // This takes the MAST value of an action, and blends it with an external (fixed) action heuristic
    public double beta; // weight of the external value
    IActionHeuristic externalHeuristic;

    public MASTPlusActionHeuristic(IActionHeuristic externalHeuristic, IActionKey actionKey, double defaultValue, double beta) {
        super(null, actionKey, defaultValue);
        this.beta = beta;
        this.externalHeuristic = externalHeuristic;
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state) {
        if (beta > 0.0)  // avoid potentially expensive computation if beta is 0
            return (1 - beta) * super.evaluateAction(action, state) + beta * externalHeuristic.evaluateAction(action, state);
        else
            return super.evaluateAction(action, state);
    }

}
