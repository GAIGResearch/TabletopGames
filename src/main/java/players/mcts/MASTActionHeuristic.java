package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import players.simple.BoltzmannActionPlayer;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
    public double evaluateAction(AbstractAction action, AbstractGameState state) {
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
