package players.rl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractGameState;
import core.actions.AbstractAction;

import players.rl.DataProcessor.Field;
import players.rl.RLPlayer.RLType;

public abstract class QWeightsDataStructure {

    public static final String qWeightsFolderName = "qWeights";

    // Alias for a sorted(!) hashmap between the state name
    // (players.rl.RLFeatureVector::names) and weight value
    protected class StateMap extends LinkedHashMap<String, Double> {
    }

    private final String infileName;
    protected Path infilePath;

    protected RLParams playerParams;
    protected RLTrainingParams trainingParams;

    private String gameFolderPath;

    public QWeightsDataStructure(String infileNameOrAbsPath) {
        this.infileName = infileNameOrAbsPath;
    }

    // Ensures agent doesn't need to load the files between every game
    private boolean initialized = false;

    protected abstract void initQWeightsEmpty();

    protected abstract void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

    protected abstract double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action);

    protected abstract void parseQWeights(StateMap stateMap);

    protected abstract StateMap qWeightsToStateMap();

    void initialize(String gameName) {
        if (initialized)
            return;
        initQWeightsEmpty();
        setPaths(gameName);
        tryReadQWeightsFromFile();
        initialized = true;
    }

    protected void initInFilePath(String defaultPath) {
        infilePath = infileName == null ? null
                : Paths.get(infileName).isAbsolute() ? Paths.get(infileName)
                        : Paths.get(defaultPath, infileName).toAbsolutePath();
    }

    public String getInfilePath() {
        return infilePath == null ? null : infilePath.toString();
    }

    private void setPaths(String gameName) {
        this.gameFolderPath = getFolderPath(gameName);
        initInFilePath(gameFolderPath);
    }

    private JsonNode tryReadQWeightsFromFile() {
        initQWeightsEmpty();
        if (getInfilePath() == null)
            return null;
        try {
            File file = new File(getInfilePath());
            JsonNode data = new ObjectMapper().readTree(file);
            if (!matchesParameters(data.get("Metadata")))
                throw new IllegalArgumentException(
                        "Metadata in file does not match provided parameters: " + file.getAbsolutePath());
            StateMap stateMap = new StateMap() {
                {
                    data.get("Weights").fields()
                            .forEachRemaining(e -> put(e.getKey(), e.getValue().asDouble()));
                }
            };
            parseQWeights(stateMap);
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    private boolean matchesParameters(JsonNode metadata) {
        if (!metadata.get(Field.QWeightsDataStructure.name()).asText()
                .equals(this.getClass().getCanonicalName()))
            return false;
        if (!metadata.get(Field.Type.name()).asText()
                .equals(playerParams.type.name()))
            return false;
        if (!metadata.get(Field.RLFeatureVector.name()).asText()
                .equals(playerParams.features.getClass().getCanonicalName()))
            return false;
        return true;
    }

    protected void qLearning(RLPlayer player, List<TurnSAR> turns) {
        // Learn
        for (int i = turns.size() - 2; i >= 0; i--) {
            // Turns at time t and t+1, respectively
            TurnSAR t0 = turns.get(i);
            TurnSAR t1 = turns.get(i + 1);

            double maxQ_s1a = t1.a == null ? 0
                    : t1.possibleActions.stream().mapToDouble(a -> evaluateQ(player, t1.s, a)).max().getAsDouble();

            // Q-Learning formula
            double q_s0a0 = evaluateQ(player, t0.s, t1.a);
            double delta = trainingParams.alpha * (t1.r + trainingParams.gamma * maxQ_s1a - q_s0a0);
            add(player, t0.s, t0.a, delta);
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

    public abstract RLType getType();

}