package players.heuristics;

import core.AbstractGameState;
import core.interfaces.*;
import utilities.Pair;
import utilities.Utils;

import java.util.Map;

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
        Pair<double[], Map<int[], Double>> x = loadModel(coefficientsFile);
        this.coefficients = x.a;
        this.interactionCoefficients = x.b;
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        // default heuristic is used if the state is terminal (or no coefficients are provided)
        if (coefficients != null && (defaultHeuristic == null || state.isNotTerminal())) {
            double[] phi = features.featureVector(state, playerId);
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
