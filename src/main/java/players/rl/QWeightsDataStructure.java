package players.rl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;

public abstract class QWeightsDataStructure {

    protected RLParams params;
    protected RLTrainingParams trainingParams;

    private String gameFolderPath;
    private String qWeightsFolderPath;

    // Ensures agent doesn't need to load the files between every game
    private boolean initialized = false;

    protected abstract void initQWeights();

    protected abstract void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

    protected abstract double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action);

    protected abstract void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1);

    protected abstract void parseQWeights(String[] qWeightStrings);

    protected abstract String qWeightsToString();

    void initialize(String gameName) {
        if (initialized)
            return;
        setPaths(gameName);
        tryReadQWeightsFromFile();
        initialized = true;
    }

    private void setPaths(String gameName) {
        this.gameFolderPath = RLPlayer.resourcesPath + gameName + "/";
        this.qWeightsFolderPath = gameFolderPath + "qWeights/";
    }

    private void tryReadQWeightsFromFile() {
        initQWeights();
        String readPath = qWeightsFolderPath + params.qWeightsFileId + ".txt";
        Path path = Paths.get(readPath);
        try {
            String[] weightStrings = Files.readString(path).split("\n");
            parseQWeights(weightStrings);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
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