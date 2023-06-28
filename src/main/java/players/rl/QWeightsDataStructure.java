package players.rl;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractGameState;
import core.actions.AbstractAction;

public abstract class QWeightsDataStructure {

    // Alias for a sorted(!) hashmap between the state name
    // (players.rl.RLFeatureVector::names) and weight value
    protected class StateMap extends LinkedHashMap<String, Double> {
    }

    protected RLParams params;
    protected RLTrainingParams trainingParams;

    private String gameFolderPath;
    private String qWeightsFolderPath;

    private String gameName;

    // Ensures agent doesn't need to load the files between every game
    private boolean initialized = false;

    protected abstract void initQWeightsEmpty();

    protected abstract void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

    protected abstract double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action);

    protected abstract void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1);

    protected abstract void parseQWeights(StateMap stateMap);

    protected abstract StateMap qWeightsToStateMap();

    void initialize(String gameName) {
        if (initialized)
            return;
        this.gameName = gameName;
        setPaths(gameName);
        tryReadQWeightsFromFile();
        initialized = true;
    }

    private void setPaths(String gameName) {
        this.gameFolderPath = RLPlayer.resourcesPath + gameName + "/";
        this.qWeightsFolderPath = gameFolderPath + "qWeights/";
    }

    private JsonNode tryReadQWeightsFromFile() {
        initQWeightsEmpty();
        if (params.qWeightsFileId == 0)
            return null;
        try {
                File file = DataProcessor.getFileByID(params.qWeightsFileId, gameName);
                JsonNode data = new ObjectMapper().readTree(file);
            StateMap stateMap = new StateMap() {
                {
                    // TODO: JsonNode::fields does not guarantee order. Maybe use arrays instead
                    // Might have to have a json object inside the array, to keep state-value pairs
                    // "Weights": [ { "name": "[stateName]", "value": [qValue] }, {...}, ... ]
                    // TODO: alternatively, use RLFeatureVector::names to maintain same order
                    Iterator<Map.Entry<String, JsonNode>> fields = data.get("Weights").fields();
                    Map.Entry<String, JsonNode> entry;
                    while (fields.hasNext()) {
                        entry = fields.next();
                        put(entry.getKey(), entry.getValue().asDouble());
                    }
                }
            };
            parseQWeights(stateMap);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    protected void qLearning(RLPlayer player, List<TurnSAR> turns) {
        // Learn
        for (int i = turns.size() - 2; i >= 0; i--) {
            TurnSAR t0 = turns.get(i);
            TurnSAR t1 = turns.get(i + 1);
            qLearning(player, t0, t1);
        }
    }

    protected void setParams(RLParams params) {
        this.params = params;
    }

    protected void setTrainingParams(RLTrainingParams trainingParams) {
        this.trainingParams = trainingParams;
    }

    public String getQWeightsFolderPath() {
        return qWeightsFolderPath;
    }

    public String getGameFolderPath() {
        return gameFolderPath;
    }

}