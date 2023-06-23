package games.dicemonastery.heuristics;

import evaluation.optimisation.TunableParameters;
import players.heuristics.ActionValueHeuristic;

public class AdvantageParams extends TunableParameters {

    public double rndWeight = 0.5;

    public AdvantageParams() {
        addTunableParameter("rndWeight", 0.5);
    }

    @Override
    protected AdvantageParams _copy() {
        return new AdvantageParams();
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof AdvantageParams) {
            return ((AdvantageParams) o).rndWeight == rndWeight;
        }
        return false;
    }

    @Override
    public ActionValueHeuristic instantiate() {
        throw new IllegalArgumentException("Not yet implemented");
    }

    @Override
    public void _reset() {
        rndWeight = (double) getParameterValue("rndWeight");
    }
}
