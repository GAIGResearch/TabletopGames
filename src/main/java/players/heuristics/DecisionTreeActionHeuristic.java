package players.heuristics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.*;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DecisionTreeActionHeuristic extends AbstractDecisionTreeHeuristic
        implements IActionHeuristic, IToJSON, IToFile {

    IStateFeatureVector stateFeatures;
    IActionFeatureVector actionFeatures;

    public DecisionTreeActionHeuristic(IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures, String directory) {
        super(directory);
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
    }

    public DecisionTreeActionHeuristic(IStateFeatureVector stateFeatures, IActionFeatureVector actionFeatures, DecisionTreeRegressionModel drModel) {
        super(drModel);
        this.stateFeatures = stateFeatures;
        this.actionFeatures = actionFeatures;
    }

    public DecisionTreeActionHeuristic(JSONObject json) {
        super((String) json.get("file"));
        JSONObject stateJSON = (JSONObject) json.get("stateFeatures");
        if (stateJSON != null)
            this.stateFeatures = JSONUtils.loadClassFromJSON((JSONObject) json.get("stateFeatures"));
        this.actionFeatures = JSONUtils.loadClassFromJSON((JSONObject) json.get("actionFeatures"));
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        if (drModel == null) return 0;  // no model, no prediction (this is fine
        // get the features for the state and action
        int playerId = state.getCurrentPlayer();
        double[] actionFeatures = this.actionFeatures.doubleVector(action, state, playerId);
        if (stateFeatures != null) {
            double[] stateFeatures = this.stateFeatures.doubleVector(state, playerId);
            double[] features = new double[stateFeatures.length + actionFeatures.length];
            System.arraycopy(stateFeatures, 0, features, 0, stateFeatures.length);
            System.arraycopy(actionFeatures, 0, features, stateFeatures.length, actionFeatures.length);
            return drModel.predict(Vectors.dense(features));
        } else {
            return drModel.predict(Vectors.dense(actionFeatures));
        }
    }

    @Override
    public double[] evaluateAllActions(List<AbstractAction> actions, AbstractGameState state) {
        if (drModel == null) return new double[actions.size()];  // no model, no prediction (this is fine)
        if (stateFeatures == null) {
            // in this case we used the default implementation, as there is no
            // benefit from a single pass for the state features
            return IActionHeuristic.super.evaluateAllActions(actions, state);
        }
        // First we get the state features once
        int playerId = state.getCurrentPlayer();
        double[] stateFeatures = this.stateFeatures.doubleVector(state, playerId);
        // Then we get the action features for each action
        double[][] actionFeatures = new double[actions.size()][];
        for (int i = 0; i < actions.size(); i++) {
            actionFeatures[i] = this.actionFeatures.doubleVector(actions.get(i), state, playerId);
        }
        // Then we combine the features
        double[][] features = new double[actions.size()][stateFeatures.length + actionFeatures[0].length];
        for (int i = 0; i < actions.size(); i++) {
            System.arraycopy(stateFeatures, 0, features[i], 0, stateFeatures.length);
            System.arraycopy(actionFeatures[i], 0, features[i], stateFeatures.length, actionFeatures[i].length);
        }
        // Then we return the predictions from the model
        double[] predictions = new double[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            predictions[i] = drModel.predict(Vectors.dense(features[i]));
        }
        return predictions;
    }

    @Override
    public void writeToFile(String file) {
        if (modelDirectory.isEmpty())
            modelDirectory = file;
        try {
            drModel.write().overwrite().save(file);
            BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(file + File.separator + "Description.txt"));
            String[] names = actionFeatures.names();
            if (stateFeatures != null) {
                names = new String[stateFeatures.names().length + actionFeatures.names().length];
                System.arraycopy(stateFeatures.names(), 0, names, 0, stateFeatures.names().length);
                System.arraycopy(actionFeatures.names(), 0, names, stateFeatures.names().length, actionFeatures.names().length);
            }
            writer.write(DecisionTreeActionHeuristic.prettifyDecisionTreeDescription(drModel, names));
            writer.close();
        } catch (IOException e) {
            System.out.println("Failed to save decision tree model");
            throw new AssertionError(drModel.toDebugString());
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", "players.heuristics.DecisionTreeActionHeuristic");
        if (!modelDirectory.isEmpty()) {
            json.put("file", modelDirectory);
        }
        if (stateFeatures != null) {
            JSONObject featuresJson = new JSONObject();
            if (stateFeatures instanceof IToJSON toJSON) {
                featuresJson = toJSON.toJSON();
            } else {
                featuresJson.put("class", stateFeatures.getClass().getName());
            }
            json.put("stateFeatures", featuresJson);
        }

        JSONObject actionFeaturesJson = new JSONObject();
        if (actionFeatures instanceof IToJSON toJSON) {
            actionFeaturesJson = toJSON.toJSON();
        } else {
            actionFeaturesJson.put("class", actionFeatures.getClass().getName());
        }
        json.put("actionFeatures", actionFeaturesJson);
        return json;
    }
}
