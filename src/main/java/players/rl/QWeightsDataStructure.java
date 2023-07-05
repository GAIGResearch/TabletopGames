package players.rl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import core.AbstractGameState;
import core.actions.AbstractAction;

import players.rl.DataProcessor.Field;
import players.rl.RLPlayer.RLType;
import utilities.Pair;

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

    private String qWeightsFolderPath;

    public QWeightsDataStructure(String infileNameOrAbsPath) {
        this.infileName = infileNameOrAbsPath;
    }

    // Ensures agent doesn't need to load the files between every game
    private boolean initialized = false;

    protected abstract void initQWeightsEmpty();

    protected abstract void applyGradient(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

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

    protected void initInfilePath() {
        if (infileName == null) {
            infilePath = null;
            return;
        }
        Path[] pathsInOrderOfCheck = {
                Paths.get(qWeightsFolderPath, infileName), // Just filename
                Paths.get(RLPlayer.resourcesPath, infileName), // Path relative to resources folder
                Paths.get(infileName), // Absolute path or relative to root folder
        };
        infilePath = Arrays.stream(pathsInOrderOfCheck).filter(p -> p.toFile().exists()).findFirst().get();
    }

    public String getInfilePathName() {
        return infilePath == null ? null : infilePath.toString();
    }

    private void setPaths(String gameName) {
        this.qWeightsFolderPath = getFolderPath(gameName);
        initInfilePath();
    }

    private JsonNode tryReadQWeightsFromFile() {
        initQWeightsEmpty();
        if (getInfilePathName() == null)
            return null;
        try {
            File file = new File(getInfilePathName());
            JsonNode data = new ObjectMapper().readTree(file);
            checkIfParametersMatch(data.get("Metadata"));
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

    private void checkIfParametersMatch(JsonNode metadata) {
        List<Pair<String, String>> texts = new LinkedList<Pair<String, String>>();
        // Type (Tabular, Linear Approx, etc.)
        texts.add(new Pair<String, String>(
                metadata.get(Field.Type.name()).asText(),
                playerParams.type.name()));
        // Feature Vector Class name
        texts.add(new Pair<String, String>(
                metadata.get(Field.FeatureVector.name()).asText(),
                playerParams.getFeatureVectorCanonicalName()));
        // Compare all entries and throw an error where relevant
        for (int i = 0; i < texts.size(); i++)
            if (texts.get(i).a != texts.get(i).b)
                throw new IllegalArgumentException("Metadata in file does not match provided parameters:\n\t"
                        + texts.get(i).a + " =/= " + texts.get(i).b);
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
            double q_s0a0 = evaluateQ(player, t0.s, t0.a);
            double delta = trainingParams.alpha * (t1.r + trainingParams.gamma * maxQ_s1a - q_s0a0);
            applyGradient(player, t0.s, t0.a, delta);
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

    public String getQWeightsFolderPath() {
        return qWeightsFolderPath;
    }

    public abstract RLType getType();

}