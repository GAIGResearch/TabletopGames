package players.mcts;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import evaluation.optimisation.TunableParameters;
import utilities.Pair;

import java.util.List;
import java.util.Map;

public class MASTPlusActionHeuristic extends TunableParameters<MASTPlusActionHeuristic> implements IActionHeuristic, IMASTUser {

    // This takes the MAST value of an action, and blends it with an external (fixed) action heuristic
    public double beta; // weight of the external value
    IActionHeuristic externalHeuristic;
    MASTActionHeuristic baseHeuristic;

    public MASTPlusActionHeuristic() {
        addTunableParameter("beta", 0.0);
        addTunableParameter("externalHeuristic", IActionHeuristic.class, externalHeuristic);
        addTunableParameter("baseHeuristic", MASTActionHeuristic.class, new MASTActionHeuristic(null, 0.0));
    }

    @Override
    protected MASTPlusActionHeuristic _copy() {
        return new MASTPlusActionHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    public MASTPlusActionHeuristic(IActionHeuristic externalHeuristic, IActionKey actionKey, double defaultValue, double beta) {
        this();
        setParameterValue("beta", beta);
        setParameterValue("externalHeuristic", externalHeuristic);
        setParameterValue("baseHeuristic", new MASTActionHeuristic(actionKey, defaultValue));
    }

    @Override
    public void _reset() {
        beta = (double) getParameterValue("beta");
        externalHeuristic = (IActionHeuristic) getParameterValue("externalHeuristic");
        baseHeuristic = (MASTActionHeuristic) getParameterValue("baseHeuristic");
    }

    @Override
    public MASTPlusActionHeuristic instantiate() {
        return this;
    }


    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> actions) {
        if (beta > 0.0)  // avoid potentially expensive computation if beta is 0
            return (1 - beta) * baseHeuristic.evaluateAction(action, state, actions) + beta * externalHeuristic.evaluateAction(action, state, actions);
        else
            return baseHeuristic.evaluateAction(action, state, actions);
    }

    @Override
    public void setMASTStats(List<Map<Object, Pair<Integer, Double>>> MASTStats) {
        baseHeuristic.setMASTStats(MASTStats);
    }
}
