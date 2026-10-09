package players.simple;

import core.AbstractParameters;
import core.interfaces.IActionHeuristic;
import evaluation.optimisation.TunableParameters;
import players.heuristics.NullActionHeuristic;
import players.mcts.MASTPlusActionHeuristic;

public class BoltzmannActionParams extends TunableParameters {

    public double temperature;
    public double epsilon;
    public double MASTBeta;
    public IActionHeuristic actionHeuristic;

    public BoltzmannActionParams() {
        addTunableParameter("temperature", 1.0);
        addTunableParameter("epsilon", 0.0);
        addTunableParameter("MASTBeta", 0.0);
        addTunableParameter("actionHeuristic", IActionHeuristic.class, new NullActionHeuristic());
    }

    @Override
    public void _reset() {
        temperature = (double) this.getParameterValue("temperature");
        epsilon = (double) this.getParameterValue("epsilon");
        MASTBeta = (double) this.getParameterValue("MASTBeta");
        actionHeuristic = (IActionHeuristic) this.getParameterValue("actionHeuristic");
    }

    @Override
    public Object instantiate() {
        IActionHeuristic actionHeuristicToUse = (MASTBeta > 0.0)
                ? new MASTPlusActionHeuristic(actionHeuristic, null, 0.0, 1.0 - MASTBeta)
                : actionHeuristic;
        return new BoltzmannActionPlayer(actionHeuristicToUse, temperature, epsilon, System.currentTimeMillis());
    }


    @Override
    protected AbstractParameters _copy() {
        return new BoltzmannActionParams();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof BoltzmannActionParams;
    }


}
