package players.mcts;

import utilities.Pair;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;

import java.util.List;
import java.util.Map;

public class MASTActionHeuristic implements IActionHeuristic {

    List<Map<Object, Pair<Integer, Double>>> MASTStatistics;
    IActionKey actionKey;
    double defaultValue;

    public MASTActionHeuristic(List<Map<Object, Pair<Integer, Double>>> MASTStatistics, IActionKey actionKey, double defaultValue) {
        this.MASTStatistics = MASTStatistics;
        this.actionKey = actionKey;
        this.defaultValue = defaultValue;
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        Object key = actionKey == null ? action : actionKey.key(action);
        Map<Object, Pair<Integer, Double>> MAST = MASTStatistics.get(state.getCurrentPlayer());
        if (MAST.containsKey(key)) {
            Pair<Integer, Double> stats = MAST.get(key);
            if (stats.a > 0)
                return stats.b / stats.a;
        }
        return defaultValue;
    }
}
