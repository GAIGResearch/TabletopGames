package players.heuristics;

import core.AbstractGameState;
import core.interfaces.*;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.regression.DecisionTreeRegressionModel;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class DecisionTreeStateHeuristic extends AbstractDecisionTreeHeuristic
        implements IStateHeuristic, IToJSON, IToFile {

    IStateFeatureVector stateFeatures;
    IStateHeuristic defaultHeuristic;

    public DecisionTreeStateHeuristic(IStateFeatureVector stateFeatures, String directory, IStateHeuristic defaultHeuristic) {
        super(directory);
        this.stateFeatures = stateFeatures;
        this.defaultHeuristic = defaultHeuristic;
    }

    public DecisionTreeStateHeuristic(IStateFeatureVector stateFeatures,
                                      DecisionTreeRegressionModel decisionTreeRegressionModel,
                                      IStateHeuristic defaultHeuristic) {
        super(decisionTreeRegressionModel);
        this.stateFeatures = stateFeatures;
        this.defaultHeuristic = defaultHeuristic;
    }

    public DecisionTreeStateHeuristic(JSONObject json) {
        super((String) json.get("file"));
        this.stateFeatures = JSONUtils.loadClassFromJSON((JSONObject) json.get("features"));
        this.defaultHeuristic = JSONUtils.loadClassFromJSON((JSONObject) json.get("defaultHeuristic"));
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        // if terminal, we use the default heuristic
        if (defaultHeuristic != null && !state.isNotTerminal()) {
            return defaultHeuristic.evaluateState(state, playerId);
        }

        if (drModel == null) return 0;  // no model, no prediction (this is fine)

        // get the features for the state
        double[] features = this.stateFeatures.doubleVector(state, playerId);

        // return the prediction from the model
        return drModel.predict(Vectors.dense(features));
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("class", "players.heuristics.DecisionTreeStateHeuristic");
        if (!modelDirectory.isEmpty()) {
            json.put("file", modelDirectory);
        }

        JSONObject featuresJson = new JSONObject();
        if (stateFeatures instanceof IToJSON toJSON) {
            featuresJson = toJSON.toJSON();
        } else {
            featuresJson.put("class", stateFeatures.getClass().getName());
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

    @Override
    public void writeToFile(String file) {
        if (modelDirectory.isEmpty())
            modelDirectory = file;
        try {
            drModel.write().overwrite().save(file);
            BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(file + File.separator + "Description.txt"));
            writer.write(DecisionTreeActionHeuristic.prettifyDecisionTreeDescription(drModel, stateFeatures.names()));
            writer.close();
        } catch (IOException e) {
            System.out.println("Failed to save decision tree model");
            throw new AssertionError(drModel.toDebugString());
        }

    }
}
