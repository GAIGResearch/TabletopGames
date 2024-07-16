package players.mcts;

import core.AbstractParameters;
import core.interfaces.IActionHeuristic;
import core.interfaces.IActionKey;
import evaluation.optimisation.TunableParameters;

public class MASTPlayerParams extends TunableParameters {

    // It is not absolutely necessary to have a separate class for the parameters of MASTPlayer, and
    // rollout using MAST can be defined directly using MCTSParams (allowing temperature and defaultValue to be set)

    // Use this class if blending in some other heuristic; or if it iis necessary to have different MAST parameterisations
    // for rollout policy and opponent models
    public IActionHeuristic externalHeuristic = (gameState, action, actions) -> 0.0;
    public double weightOfExternal = 0.5;
    public double temperature = 1.0;
    public double epsilon = 0.0;
    public double defaultValue = 0.0;
    public IActionKey actionKey;
    public MASTPlayerParams() {
        addTunableParameter("externalHeuristic", (IActionHeuristic) (gameState, action, actions) -> 0.0);
        addTunableParameter("weightOfExternal", 0.5);
        addTunableParameter("temperature", 1.0);
        addTunableParameter("epsilon", 0.0);
        addTunableParameter("defaultValue", 0.0);
        addTunableParameter("actionKey", IActionKey.class);
    }

    @Override
    public void _reset() {
        externalHeuristic = (IActionHeuristic) this.getParameterValue("externalHeuristic");
        weightOfExternal = (double) this.getParameterValue("weightOfExternal");
        temperature = (double) this.getParameterValue("temperature");
        epsilon = (double) this.getParameterValue("epsilon");
        defaultValue = (double) this.getParameterValue("defaultValue");
        actionKey = (IActionKey) this.getParameterValue("actionKey");
    }

    @Override
    public Object instantiate() {
        return new MASTPlayer(externalHeuristic, weightOfExternal, actionKey, temperature, epsilon, defaultValue);
    }

    @Override
    protected AbstractParameters _copy() {
        return new MASTPlayerParams();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof MASTPlayerParams;
    }
}
