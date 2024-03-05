package players.heuristics;

import core.interfaces.ICoefficients;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import utilities.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a wrapper around an IStateFeatureVector and an array of coefficients
 */
public abstract class AbstractStateHeuristic implements IStateHeuristic, ICoefficients {

    protected IStateFeatureVector features;

    @Override
    public String[] names() {
        return features.names();
    }
    protected double[] coefficients;
    protected Map<int[], Double> interactionCoefficients = new HashMap<>();
    protected IStateHeuristic defaultHeuristic;

    @Override
    public double[] coefficients() {
        return coefficients;
    }
    @Override
    public Map<int[], Double> interactionCoefficients() {
        return interactionCoefficients;
    }

    public AbstractStateHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClassName) {
        try {
            features = (IStateFeatureVector) Class.forName(featureVectorClassName).getConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError("Problem with Class : " + featureVectorClassName);
        }
        if (defaultHeuristicClassName.isEmpty()) {
            defaultHeuristic = new LeaderHeuristic();
        } else {
            try {
                defaultHeuristic = (IStateHeuristic) Class.forName(defaultHeuristicClassName).getConstructor().newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError("Problem with Class : " + defaultHeuristicClassName);
            }
        }
        Pair<double[], Map<int[], Double>> x =  loadModel(coefficientsFile);
        this.coefficients = x.a;
        this.interactionCoefficients = x.b;
    }

    public AbstractStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        this.features = featureVector;
        this.defaultHeuristic = defaultHeuristic;
        Pair<double[], Map<int[], Double>> x = loadModel(coefficientsFile);
        this.coefficients = x.a;
        this.interactionCoefficients = x.b;
    }

}
