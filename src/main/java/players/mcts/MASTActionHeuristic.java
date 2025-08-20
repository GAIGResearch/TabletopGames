package players.mcts;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import core.interfaces.ITunableParameters;
import evaluation.optimisation.TunableParameters;
import players.simple.BoltzmannActionPlayer;
import utilities.Pair;

import java.util.*;

public class MASTActionHeuristic extends TunableParameters<MASTActionHeuristic> implements IActionHeuristic, IMASTUser {

    List<Map<Object, Pair<Integer, Double>>> MASTStatistics = new ArrayList<>();
    IActionKey actionKey; // null is fine; this indicates to use the Action as the Key
    double defaultValue;

    public MASTActionHeuristic() {
        addTunableParameter("actionKey",  IActionKey.class, (Object) null);
        addTunableParameter("defaultValue",  0.0);
    }
    
    public MASTActionHeuristic(IActionKey actionKey, double defaultValue) {
        this();
        setParameterValue("actionKey", actionKey);
        setParameterValue("defaultValue", defaultValue);
    }

    @Override
    public void _reset() {
        actionKey = (IActionKey) getParameterValue("actionKey");
        defaultValue = (double) getParameterValue("defaultValue");
    }
    
    public void setMASTStats(List<Map<Object, Pair<Integer, Double>>> MASTStatistics) {
        this.MASTStatistics = MASTStatistics;
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

    @Override
    protected MASTActionHeuristic _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof MASTActionHeuristic &&
               Arrays.equals(MASTStatistics.toArray(), ((MASTActionHeuristic) o).MASTStatistics.toArray());
    }

    @Override
    public MASTActionHeuristic instantiate() {
        return this;
    }

}
