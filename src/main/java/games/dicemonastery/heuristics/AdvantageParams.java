package games.dicemonastery.heuristics;

import evaluation.TunableParameters;

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
    public Advantage002 instantiate() {
        return new Advantage002();
    }

    @Override
    public void _reset() {
        rndWeight = (double) getParameterValue("rndWeight");
    }
}
