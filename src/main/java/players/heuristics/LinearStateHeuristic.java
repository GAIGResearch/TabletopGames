package players.heuristics;

import core.AbstractGameState;
import core.interfaces.*;
import utilities.Utils;

public class LinearStateHeuristic extends GLMHeuristic implements IStateHeuristic {

    protected IStateFeatureVector features;
    protected IStateHeuristic defaultHeuristic;

    @Override
    public String[] names() {
        return features.names();
    }

    public LinearStateHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        this.features = featureVector;
        this.defaultHeuristic = defaultHeuristic;
        loadFromFile(coefficientsFile);
    }
    public LinearStateHeuristic(IStateFeatureVector featureVector, double[] coefficients, IStateHeuristic defaultHeuristic) {
        this.features = featureVector;
        this.defaultHeuristic = defaultHeuristic;
        this.coefficients = coefficients;
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        // default heuristic is used if the state is terminal (or no coefficients are provided)
        if (coefficients != null && (defaultHeuristic == null || state.isNotTerminal())) {
            double[] phi = features.doubleVector(state, playerId);
            double retValue = inverseLinkFunction.applyAsDouble(applyCoefficients(phi));
            if (defaultHeuristic != null)
                return Utils.clamp(retValue, defaultHeuristic.minValue(), defaultHeuristic.maxValue());
            return retValue;
        }
        if (defaultHeuristic != null)
            return defaultHeuristic.evaluateState(state, playerId);
        return 0;
    }


}
