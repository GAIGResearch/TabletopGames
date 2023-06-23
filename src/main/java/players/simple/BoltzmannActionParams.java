package players.simple;

import core.AbstractParameters;
import core.interfaces.IActionHeuristic;
import evaluation.TunableParameters;

public class BoltzmannActionParams extends TunableParameters {

    public double temperature;
    public double epsilon;
    public IActionHeuristic actionHeuristic;

    public BoltzmannActionParams() {
        addTunableParameter("temperature", 1.0);
        addTunableParameter("epsilon", 0.0);
        addTunableParameter("actionHeuristic", (IActionHeuristic) (gameState, action) -> 0.0);
    }

    @Override
    public void _reset() {
        temperature = (double) this.getParameterValue("temperature");
        epsilon = (double) this.getParameterValue("epsilon");
        actionHeuristic = (IActionHeuristic) this.getParameterValue("actionHeuristic");
    }

    @Override
    public Object instantiate() {
        return new BoltzmannActionPlayer(actionHeuristic, temperature, epsilon, System.currentTimeMillis());
    }


    @Override
    protected AbstractParameters _copy() {
        BoltzmannActionParams bap = new BoltzmannActionParams();
        bap.setParameterValue("temperature", temperature);
        bap.setParameterValue("epsilon", epsilon);
        bap.setParameterValue("actionHeuristic", actionHeuristic);
        bap._reset();
        return bap;
    }

    @Override
    protected boolean _equals(Object o) {
        return o == this;
    }


}
