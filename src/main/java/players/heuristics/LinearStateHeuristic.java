package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import utilities.Utils;


public class LinearStateHeuristic extends AbstractStateHeuristic {

    protected double minValue = Double.NEGATIVE_INFINITY;
    protected double maxValue = Double.POSITIVE_INFINITY;

    public LinearStateHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClass) {
        super(featureVectorClassName, coefficientsFile, defaultHeuristicClass);
    }
    public LinearStateHeuristic(String featureVectorClassName, String coefficientsFile) {
        super(featureVectorClassName, coefficientsFile, "");
    }
    public LinearStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        if (coefficients == null)
            return defaultHeuristic.evaluateState(state, playerId);
        double[] phi = features.featureVector(state, playerId);
        double retValue = coefficients[0]; // the bias term
        for (int i = 0; i < phi.length; i++) {
            retValue += phi[i] * coefficients[i+1];
        }
        return Utils.range(retValue, minValue, maxValue);
    }
}
