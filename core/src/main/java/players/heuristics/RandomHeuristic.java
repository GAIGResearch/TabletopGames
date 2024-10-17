package players.heuristics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;

import java.util.List;
import java.util.Random;

public class RandomHeuristic implements IActionHeuristic {

    private Random rnd;

    public RandomHeuristic(int seed) {
        rnd = new Random(seed);
    }

    @Override
    public double evaluateAction(AbstractAction abstractAction, AbstractGameState abstractGameState, List<AbstractAction> contextActions) {
        return rnd.nextDouble();
    }
}
