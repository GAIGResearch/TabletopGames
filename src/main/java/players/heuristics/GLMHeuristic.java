package players.heuristics;

import core.interfaces.ICoefficients;
import core.interfaces.IHasName;
import org.apache.spark.ml.regression.GeneralizedLinearRegressionModel;
import org.json.simple.JSONObject;
import utilities.Pair;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

public abstract class GLMHeuristic implements ICoefficients, IHasName {

    /**
     * A Generalised Linear Model (GLM) State Heuristic
     * This defaults to a linear model (and link function), but this can be amended by setting the inverseLinkFunction
     * (In a GLM we have g(mu) = X*beta, where g is the link function, mu is the expected value of the response variable)
     * Hence given the coefficients, beta, we need the inverse link function to get the expected value of the response variable
     */
    protected String name = "";

    protected double[] coefficients;

    protected double[] interactionCoefficients;
    protected int[][] interactions;

    protected DoubleUnaryOperator inverseLinkFunction = x -> x;  // default to linear link function

    // This is not actually used, but is available immediately after training
    protected GeneralizedLinearRegressionModel underlyingModel;

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

    public void loadCoefficientsFromJSON(JSONObject json) {
        // Coefficients to be pulled in from JSON
        if (json.get("coefficients") instanceof JSONObject coefficientsAsJSON) {
            Pair<double[], List<Pair<int[], Double>>> coeffs = this.coefficientsFromJSON(coefficientsAsJSON);
            coefficients = coeffs.a;
            interactions = coeffs.b.stream().map(p -> p.a).toArray(int[][]::new);
            interactionCoefficients = coeffs.b.stream().mapToDouble(p -> p.b).toArray();
        } else if (json.get("coefficients") instanceof String coefficientsFile) {
            loadFromFile(coefficientsFile);
        } else {
            throw new IllegalArgumentException("Coefficients must be a JSON array or a file name");
        }
    }

    public void loadFromFile(String coefficientsFile) {
        Pair<double[], List<Pair<int[], Double>>> x = loadModel(coefficientsFile);
        this.coefficients = x.a;
        if (x.b.isEmpty())
            return;
        this.interactions = new int[x.b.size()][];
        this.interactionCoefficients = new double[x.b.size()];
        for (int i = 0; i < interactions.length; i++) {
            interactions[i] = x.b.get(i).a;
            interactionCoefficients[i] = x.b.get(i).b;
        }
    }

    public GeneralizedLinearRegressionModel getModel() {
        return underlyingModel;
    }

    public GLMHeuristic setModel(GeneralizedLinearRegressionModel model) {
        this.underlyingModel = model;
        double[] coeffs = model.coefficients().toArray();
        coefficients = new double[coeffs.length + 1];
        coefficients[0] = model.intercept();
        System.arraycopy(coeffs, 0, coefficients, 1, coeffs.length);
        return this;
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + " : " + getClass().getSimpleName();
    }
}
