package players.heuristics;

import core.interfaces.IActionFeatureVector;
import core.interfaces.IActionHeuristic;
import core.interfaces.ICoefficients;
import core.interfaces.IStateFeatureVector;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a wrapper around an IStateFeatureVector and an array of coefficients
 */
public abstract class GLMActionHeuristic implements IActionHeuristic, ICoefficients {

    protected IStateFeatureVector features;
    protected IActionFeatureVector actionFeatures;

    String[] names;
    protected double[] coefficients;
    protected Map<int[], Double> interactionCoefficients = new HashMap<>();

    @Override
    public String[] names() {
        return names;
    }
    @Override
    public double[] coefficients() {
        return coefficients;
    }
    @Override
    public Map<int[], Double> interactionCoefficients() {
        return interactionCoefficients;
    }


    /**
     * The coefficientsFile is a tab separated file with the first line being the names of the features
     * and the second line being the coefficients.
     * <p>
     * The convention required is that the State coefficients are first, followed by the Action coefficients.
     *
     * @param featureVector
     * @param actionFeatureVector
     * @param coefficientsFile
     */
    public GLMActionHeuristic(IActionFeatureVector actionFeatureVector, IStateFeatureVector featureVector, String coefficientsFile) {
        this.features = featureVector;
        this.actionFeatures = actionFeatureVector;
        // then add on the action feature names
        names = new String[features.names().length + actionFeatures.names().length];
        System.arraycopy(features.names(), 0, names, 0, features.names().length);
        System.arraycopy(actionFeatures.names(), 0, names, features.names().length, actionFeatures.names().length);
        loadModel(coefficientsFile);
    }


}
