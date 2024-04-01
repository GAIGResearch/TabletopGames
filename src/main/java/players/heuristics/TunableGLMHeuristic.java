package players.heuristics;

import core.AbstractParameters;
import core.interfaces.ICoefficients;
import evaluation.optimisation.TunableParameters;

import java.util.Map;

public class TunableGLMHeuristic extends TunableParameters implements ICoefficients {

    public final GLMHeuristic heuristic;

    public enum LinkFunction {
        Linear, Logistic
    }

    public TunableGLMHeuristic(GLMHeuristic heuristic) {
        this.heuristic = heuristic;
        // we initialise with a heuristic, which needs to define the feature spaces to be used
        addTunableParameter("coefficients", "None");
        addTunableParameter("linkFunction", LinkFunction.Linear);
    }

    public void _reset() {
        String coefficientsFile = (String) this.getParameterValue("coefficients");
        if (!coefficientsFile.equals("None")) {
            heuristic.loadModel(coefficientsFile);
        }
        LinkFunction linkFunction = (LinkFunction) this.getParameterValue("linkFunction");
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
    public String[] names() {
        return heuristic.names();
    }

    @Override
    public double[] coefficients() {
        return heuristic.coefficients;
    }

    @Override
    public Map<int[], Double> interactionCoefficients() {
        return heuristic.interactionCoefficients;
    }

    @Override
    protected AbstractParameters _copy() {
        return new TunableGLMHeuristic(heuristic);
    }

}
