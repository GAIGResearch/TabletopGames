package players.heuristics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.*;
import evaluation.features.AutomatedFeatures;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Provides a wrapper around an IStateFeatureVector and an array of coefficients
 */
public class LinearActionHeuristic extends GLMHeuristic implements IActionHeuristic, IToJSON {

    protected IStateFeatureVector features;
    protected IActionFeatureVector actionFeatures;

    String[] names;

    @Override
    public String[] names() {
        return names;
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
    public LinearActionHeuristic(IActionFeatureVector actionFeatureVector, IStateFeatureVector featureVector, String coefficientsFile) {
        this.features = featureVector;
        this.actionFeatures = actionFeatureVector;
        setUpNames();
        loadFromFile(coefficientsFile);
    }

    private void setUpNames() {
        if (features == null) {
            names = new String[actionFeatures.names().length];
            System.arraycopy(actionFeatures.names(), 0, names, 0, actionFeatures.names().length);
        } else {
            names = new String[features.names().length + actionFeatures.names().length];
            System.arraycopy(features.names(), 0, names, 0, features.names().length);
            System.arraycopy(actionFeatures.names(), 0, names, features.names().length, actionFeatures.names().length);
        }
    }

    public LinearActionHeuristic(IActionFeatureVector actionFeatureVector, IStateFeatureVector featureVector, double[] coefficients) {
        this.features = featureVector;
        this.actionFeatures = actionFeatureVector;
        setUpNames();
        this.coefficients = coefficients;
    }

    public LinearActionHeuristic(JSONObject json) {
        // Much the same logic as LinearStateHeuristic
        // except that the state features are optional
        if (json.get("features") != null)
            this.features = JSONUtils.loadClassFromJSON((JSONObject) json.get("features"));
        this.actionFeatures = JSONUtils.loadClassFromJSON((JSONObject) json.get("actionFeatures"));
        setUpNames();
        loadCoefficientsFromJSON(json);
    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", "players.heuristics.LinearActionHeuristic");
        JSONObject coefficientsAsJSON = coefficientsAsJSON();
        json.put("coefficients", coefficientsAsJSON);
        // Special case. While learning the coefficients, we have a single AutomatedFeatures across both
        // the state and action features. However, when we save the coefficients, we need to separate them.
        if (features != null) {
            if (features instanceof IToJSON toJSON) {
                JSONObject featuresJson = toJSON.toJSON();
                ICoefficients.removeUnusedFeatures(coefficientsAsJSON, featuresJson);
                if (features instanceof AutomatedFeatures af) {
                    // now remove the action-specific features
                    filterFeatures(featuresJson, Arrays.asList(af.underlyingAction.names()));
                }
                json.put("features", featuresJson);
            } else {
                json.put("features", features.getClass().getName());
            }
        }
        if (actionFeatures instanceof IToJSON toJSON) {
            JSONObject actionFeaturesJson = toJSON.toJSON();
            ICoefficients.removeUnusedFeatures(coefficientsAsJSON, actionFeaturesJson);
            if (actionFeatures instanceof AutomatedFeatures af) {
                // now remove the state-specific features
                filterFeatures(actionFeaturesJson, Arrays.asList(af.underlyingState.names()));
            }
            json.put("actionFeatures", actionFeaturesJson);
        } else {
            json.put("actionFeatures", actionFeatures.getClass().getName());
        }
        return json;
    }

    private void filterFeatures(JSONObject startingFeatures, List<String> featureNames) {
        JSONArray featuresArray = (JSONArray) startingFeatures.get("features");
        JSONArray filteredArray = new JSONArray();
        for (Object o : featuresArray) {
            if (o instanceof JSONObject feature) {
                String nameStem = ((String) feature.get("name")).split("\\_")[0];
                if (!featureNames.contains(nameStem)) {
                    filteredArray.add(feature);
                }
            } else {
                throw new AssertionError("Expected a JSONObject in the features array, but got: " + o.getClass().getName());
            }
        }
        startingFeatures.put("features", filteredArray);
    }

    @Override
    public double[] evaluateAllActions(List<AbstractAction> actions, AbstractGameState state) {
        if (coefficients == null)
            throw new AssertionError("No coefficients found");
        double[] retValue = new double[actions.size()];
        double[] phi = features == null ? new double[0] : features.doubleVector(state, state.getCurrentPlayer());
        for (AbstractAction action : actions) {
            double[] combined = mergePhiAndPsi(state, phi, action);
            retValue[actions.indexOf(action)] = inverseLinkFunction.applyAsDouble(applyCoefficients(combined));
        }
        return retValue;
    }

    private double[] mergePhiAndPsi(AbstractGameState state, double[] phi, AbstractAction action) {
        double[] psi = actionFeatures.doubleVector(action, state, state.getCurrentPlayer());
        double[] combined = new double[phi.length + psi.length];
        System.arraycopy(phi, 0, combined, 0, phi.length);
        System.arraycopy(psi, 0, combined, phi.length, psi.length);
        return combined;
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        if (coefficients == null)
            throw new AssertionError("No coefficients found");
        double[] phi = features == null ? new double[0] : features.doubleVector(state, state.getCurrentPlayer());
        double[] combined = mergePhiAndPsi(state, phi, action);
        return inverseLinkFunction.applyAsDouble(applyCoefficients(combined));
    }

}
