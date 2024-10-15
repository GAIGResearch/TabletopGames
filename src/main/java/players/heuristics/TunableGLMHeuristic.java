package players.heuristics;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.ICoefficients;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

import java.util.List;
import java.util.Map;

public class TunableGLMHeuristic extends TunableParameters implements IActionHeuristic, IStateHeuristic {

    public GLMHeuristic heuristic;

    public enum LinkFunction {
        Linear, Logistic
    }

    public TunableGLMHeuristic() {
        addTunableParameter("heuristic", GLMHeuristic.class);
        // we initialise with a heuristic, which needs to define the feature spaces to be used
        addTunableParameter("coefficients", "None");
        addTunableParameter("linkFunction", LinkFunction.Linear);
    }

    public void _reset() {
        heuristic = (GLMHeuristic) this.getParameterValue("heuristic");
        String coefficientsFile = (String) this.getParameterValue("coefficients");
        if (heuristic != null && !coefficientsFile.equals("None")) {
            heuristic.loadModel(coefficientsFile);
        }
        LinkFunction linkFunction = (LinkFunction) this.getParameterValue("linkFunction");
        if (heuristic != null)
            switch (linkFunction) {
                case Linear:
                    heuristic.setInverseLinkFunction(x -> x);
                    break;
                case Logistic:
                    heuristic.setInverseLinkFunction(x -> 1.0 / (1.0 + Math.exp(-x)));
                    break;
            }
    }

    @Override
    public Object instantiate() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof TunableGLMHeuristic;
    }

    @Override
    protected AbstractParameters _copy() {
        return new TunableGLMHeuristic();
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        if (heuristic instanceof IActionHeuristic actionHeuristic)
            return actionHeuristic.evaluateAction(action, state, contextActions);
        throw new AssertionError("Heuristic is not an IActionHeuristic");
    }

    @Override
    public double[] evaluateAllActions(List<AbstractAction> actions, AbstractGameState state) {
        if (heuristic instanceof IActionHeuristic actionHeuristic)
            return actionHeuristic.evaluateAllActions(actions, state);
        throw new AssertionError("Heuristic is not an IActionHeuristic");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (heuristic instanceof IStateHeuristic stateHeuristic)
            return stateHeuristic.evaluateState(gs, playerId);
        throw new AssertionError("Heuristic is not an IStateHeuristic");
    }

}
