package players.heuristics;

import core.AbstractGameState;
import core.interfaces.*;
import org.json.simple.JSONObject;
import utilities.JSONUtils;
import utilities.Utils;

public class LinearStateHeuristic extends GLMHeuristic implements IStateHeuristic, IToJSON {

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

    public LinearStateHeuristic(JSONObject json) {
        this.features = JSONUtils.loadClassFromJSON((JSONObject) json.get("features"));
        this.defaultHeuristic = JSONUtils.loadClassFromJSON((JSONObject) json.get("defaultHeuristic"));
        loadCoefficientsFromJSON(json);
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

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", "players.heuristics.LinearStateHeuristic");

        JSONObject coefficientsAsJSON = coefficientsAsJSON();
        json.put("coefficients", coefficientsAsJSON);
        JSONObject featuresJson = new JSONObject();
        if (features instanceof IToJSON toJSON) {
            featuresJson = toJSON.toJSON();
            ICoefficients.removeUnusedFeatures(coefficientsAsJSON, featuresJson);
        } else {
            featuresJson.put("class", features.getClass().getName());
        }
        json.put("features", featuresJson);
        if (defaultHeuristic != null) {
            if (defaultHeuristic instanceof IToJSON toJSON) {
                json.put("defaultHeuristic", toJSON.toJSON());
            } else {
                JSONObject defaultHeuristicJson = new JSONObject();
                defaultHeuristicJson.put("class", defaultHeuristic.getClass().getName());
                json.put("defaultHeuristic", defaultHeuristicJson);
            }
        }
        return json;
    }

    public IStateHeuristic getDefaultHeuristic() {
        return defaultHeuristic;
    }

}
