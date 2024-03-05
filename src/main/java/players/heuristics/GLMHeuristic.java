package players.heuristics;

import core.interfaces.ICoefficients;
import core.interfaces.IStateHeuristic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

public abstract class GLMHeuristic implements ICoefficients {

    /**
     * A Generalised Linear Model (GLM) State Heuristic
     * This defaults to a linear model (and link function), but this can be amended by setting the inverseLinkFunction
     * (In a GLM we have g(mu) = X*beta, where g is the link function, mu is the expected value of the response variable)
     * Hence given the coefficients, beta, we need the inverse link function to get the expected value of the response variable
     */

    protected double[] coefficients;
    protected Map<int[], Double> interactionCoefficients = new HashMap<>();

    protected DoubleUnaryOperator inverseLinkFunction = x -> x;  // default to linear link function

    @Override
    public double[] coefficients() {
        return coefficients;
    }

    @Override
    public Map<int[], Double> interactionCoefficients() {
        return interactionCoefficients;
    }
    public void setInverseLinkFunction(DoubleUnaryOperator inverseLinkFunction) {
        this.inverseLinkFunction = inverseLinkFunction;
    }

}
