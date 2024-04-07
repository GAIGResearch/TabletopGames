package players.heuristics;

import core.interfaces.ICoefficients;
import core.interfaces.IStateHeuristic;
import utilities.Pair;

import java.util.HashMap;
import java.util.List;
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

    protected double[] interactionCoefficients;
    protected int[][] interactions;

    protected DoubleUnaryOperator inverseLinkFunction = x -> x;  // default to linear link function

    @Override
    public double[] coefficients() {
        return coefficients;
    }

    @Override
    public double[] interactionCoefficients() {
        return interactionCoefficients;
    }
    @Override
    public int[][] interactions() {
        return interactions;
    }
    public void setInverseLinkFunction(DoubleUnaryOperator inverseLinkFunction) {
        this.inverseLinkFunction = inverseLinkFunction;
    }

    public void loadFromFile(String coefficientsFile) {
        Pair<double[], Map<int[], Double>> x = loadModel(coefficientsFile);
        this.coefficients = x.a;
        List<int[]> loadedInteractions = x.b.keySet().stream().toList();
        this.interactions = new int[loadedInteractions.size()][loadedInteractions.get(0).length];
        this.interactionCoefficients = new double[loadedInteractions.size()];
        for (int i = 0; i < interactions.length; i++) {
            interactions[i] = loadedInteractions.get(i);
            interactionCoefficients[i] = x.b.get(loadedInteractions.get(i));
        }
    }

}
