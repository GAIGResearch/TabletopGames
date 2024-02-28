package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import utilities.Utils;


public class LinearStateHeuristic extends AbstractStateHeuristic {

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
        // default heuristic is used if the state is terminal (or no coefficients are provided)
        if (coefficients != null && (defaultHeuristic == null || state.isNotTerminal())) {
            double[] phi = features.featureVector(state, playerId);
            double retValue = applyCoefficients(phi);
            if (defaultHeuristic != null)
                return Utils.clamp(retValue, defaultHeuristic.minValue(), defaultHeuristic.maxValue());
            return retValue;
        }
        if (defaultHeuristic != null)
            return defaultHeuristic.evaluateState(state, playerId);
        return 0;
    }


}
