package players.rl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractGameState;
import core.actions.AbstractAction;

public abstract class QWeightsDataStructure {

    public static final String qWeightsFolderName = "qWeights";

    // Alias for a sorted(!) hashmap between the state name
    // (players.rl.RLFeatureVector::names) and weight value
    protected class StateMap extends LinkedHashMap<String, Double> {
    }

    protected QWDSParams params;
    protected RLParams playerParams;
    protected RLTrainingParams trainingParams;

    private String gameFolderPath;

    public QWeightsDataStructure(QWDSParams params) {
        this.params = params;
        initQWeightsEmpty();
    }

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
        setPaths(gameName);
        tryReadQWeightsFromFile();
        initialized = true;
    }

    private void setPaths(String gameName) {
        this.gameFolderPath = getFolderPath(gameName);
        params.initInFilePath(gameFolderPath);
    }

    private JsonNode tryReadQWeightsFromFile() {
        initQWeightsEmpty();
        if (params.getInfilePath() == null)
            return null;
        try {
            File file = new File(params.getInfilePath());
            JsonNode data = new ObjectMapper().readTree(file);
            StateMap stateMap = new StateMap() {
                {
                    data.get("Weights").fields()
                            .forEachRemaining(e -> put(e.getKey(), e.getValue().asDouble()));
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

    protected void setPlayerParams(RLParams params) {
        this.playerParams = params;
    }

    protected void setTrainingParams(RLTrainingParams trainingParams) {
        this.trainingParams = trainingParams;
    }

    String getFolderPath(String gameName) {
        return Paths.get(RLPlayer.resourcesPath, qWeightsFolderName, gameName, playerParams.type.name()).toString();
    }

    public String getGameFolderPath() {
        return gameFolderPath;
    }

}