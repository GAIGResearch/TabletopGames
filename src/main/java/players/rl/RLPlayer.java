package players.rl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.FileWriter;

class SAR {
    public final AbstractGameState s;
    public final AbstractAction a;
    public final float r;

    SAR(AbstractGameState s, AbstractAction a, float r) {
        this.s = s;
        this.a = a;
        this.r = r;
    }
}

public class RLPlayer extends AbstractPlayer {

    final String weightsPath = "./beta.txt";

    RLFeatureVector features;
    List<SAR> SARs = new ArrayList<SAR>();

    final public RLParams params;

    double[] beta = null;

    public RLPlayer(RLParams params) {
        this.params = params;
        this.features = params.features;
    }

    private double solve(double[] featureVector) {
        // TODO switch between params.solver (Q-Learning, SARSA, etc.)
        // For now always Q Learning
        // TODO add default value to params
        return 0;
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        if (fileExists(weightsPath)) {
            try {
                String weights = loadFileContent(weightsPath);
                beta = Arrays.stream(weights.split("\n")).mapToDouble(Double::parseDouble).toArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void finalizePlayer(AbstractGameState gameState) {
        String outputText = "";
        for (double b : beta)
            outputText += b + "\n";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(weightsPath))) {
            writer.write(outputText);
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        int playerID = gameState.getCurrentPlayer();

        // TODO initialize beta earlier - need number of features
        if (beta == null)
            beta = new double[features.featureVector(possibleActions.get(0), gameState, playerID).length];

        // TODO choose action based on epsln-greedy policy
        AbstractAction chosenAction = possibleActions.stream()
                .max(Comparator.comparing(a -> solve(features.featureVector(a, gameState, playerID)))).get();
        SARs.add(new SAR(gameState, chosenAction, 0));
        return chosenAction;
    }

    @Override
    public AbstractPlayer copy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    public static boolean fileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isRegularFile(path);
    }

    public static String loadFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }

}
