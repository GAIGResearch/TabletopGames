package players.rl.dataStructures;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLPlayer;
import players.rl.RLTrainer;

public abstract class QWeightsDataStructure {

    protected RLTrainer trainer;

    public QWeightsDataStructure() {
        initQWeights();
    }

    public QWeightsDataStructure(RLTrainer trainer) {
        this();
        this.trainer = trainer;
    }

    public abstract void initQWeights();

    public abstract void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

    public abstract double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action);

    public abstract void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1);

    public abstract void parseQWeightsTextFile(String[] qWeightStrings);

    public abstract String qWeightsToString();

    public void qLearning(RLPlayer player, List<TurnSAR> turns) {
        // Learn
        for (int i = turns.size() - 2; i >= 0; i--) {
            TurnSAR t0 = turns.get(i);
            TurnSAR t1 = turns.get(i + 1);
            qLearning(player, t0, t1);
        }
    }

    public void tryReadBetaFromFile(String readPath) {
        Path path = Paths.get(readPath);
        if (Files.exists(path) && Files.isRegularFile(path)) {
            try {
                String[] weightStrings = Files.readString(path).split("\n");
                parseQWeightsTextFile(weightStrings);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeBetaToFile(String resourcesPath, String gameName) {
        // Write beta to the file
        String outputText = qWeightsToString();
        String writePath = resourcesPath + gameName + "/beta.txt";
        try {
            // Create directory if doesn't exist
            Path gameFolder = Path.of(resourcesPath, gameName);
            if (!Files.exists(gameFolder))
                Files.createDirectories(gameFolder);
            BufferedWriter writer = new BufferedWriter(new FileWriter(writePath));
            writer.write(outputText);
            writer.close();
        } catch (IOException e) {
            System.out.println("An error occurred while writing beta to the file: " + e.getMessage());
        }
    }

}