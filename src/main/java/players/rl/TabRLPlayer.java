package players.rl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants.GameResult;
import core.actions.AbstractAction;
import core.interfaces.IStateFeatureVector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.FileWriter;

class Turn {
    public final AbstractGameState s;
    public final AbstractAction a;
    public final float r;

    public final List<AbstractAction> possibleActions;

    Turn(AbstractGameState s, AbstractAction a, List<AbstractAction> possibleActions, float r) {
        this.s = s;
        this.a = a;
        this.r = r;
        this.possibleActions = possibleActions;
    }
}

public class TabRLPlayer extends AbstractPlayer {

    private int pId = -1; // player id

    private Random rng;

    final String weightsPath = "src/main/java/players/rl/beta.txt";

    List<Turn> turns = new ArrayList<Turn>();

    final public TabRLParams params;
    final public IStateFeatureVector features;

    Map<String, Double> beta;

    public TabRLPlayer(TabRLParams params) {
        this.params = params;
        this.features = params.features;
        this.beta = new HashMap<>();
        rng = new Random(params.getRandomSeed());
    }

    private double evaluate(AbstractGameState state, int playerId) {
        // TODO switch between params.solver (Q-Learning, SARSA, etc.)
        // For now always Q Learning
        String stateId = getStateId(state, playerId);
        // TODO add default value to params
        return beta.getOrDefault(stateId, 2.0);
    }

    private String getStateId(AbstractGameState state, int playerId) {
        double[] featureVector = features.featureVector(state, playerId);
        return Arrays.toString(featureVector).replaceAll(" ", "");
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        if (fileExists(weightsPath)) {
            try {
                String weights = loadFileContent(weightsPath);
                Arrays.stream(weights.split("\n")).forEach(s -> {
                    String[] entry = s.split(":");
                    beta.put(entry[0], Double.parseDouble(entry[1]));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void qLearning(AbstractGameState finalState) {
        // Learn
        Collections.reverse(turns);
        for (int i = 1; i < turns.size(); i++) {
            Turn t0 = turns.get(i);
            Turn t1 = turns.get(i - 1);

            AbstractGameState s0 = t0.s.copy();
            AbstractGameState s1 = t1.s;

            // TODO more sophisticated reward?
            double maxQ_s1a = i == 1
                    ? 0
                    : t1.possibleActions.stream().mapToDouble(a -> {
                        AbstractGameState copyState = s1.copy(pId);
                        getForwardModel().next(copyState, a);
                        return evaluate(copyState, pId);
                    }).max().getAsDouble();

            // ASK this is state0 BEFORE agents turn, so it doesnt match with whats in beta
            getForwardModel().next(s0, t0.a);
            String stateId0 = getStateId(s0, pId);
            double q_s0a0 = beta.getOrDefault(stateId0, 0.0);
            // Q-Learning formula
            q_s0a0 = q_s0a0 + params.alpha * (t1.r + params.gamma * maxQ_s1a - q_s0a0);
            beta.put(stateId0, q_s0a0);
        }
    }

    @Override
    public void finalizePlayer(AbstractGameState gameState) {
        float reward = gameState.getPlayerResults()[pId] == GameResult.WIN_GAME ? 1 : -1;
        turns.add(new Turn(gameState, null, null, reward));
        // TODO implement other methods than qLearning
        qLearning(gameState);
        // Write beta to the file
        // ASK do i need to add the final gameState to beta, with q = reward?
        List<String> outputs = new LinkedList<String>();
        for (String stateId : beta.keySet())
            if (beta.containsKey(stateId))
                outputs.add(stateId + ":" + beta.get(stateId));
        outputs.sort(Comparator.comparingDouble((String s) -> Double.parseDouble(s.split(":")[1])).reversed());
        String outputText = "";
        for (String s : outputs)
            outputText += s + "\n";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(weightsPath))) {
            writer.write(outputText);
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }
    }

    private AbstractAction randArgmaxEvaluation(AbstractGameState gameState, List<AbstractAction> possibleActions,
            int playerId) {
        // Choose an action that maximizes the Q-function
        List<AbstractAction> maxActions = new LinkedList<AbstractAction>();
        double maxValue = -Double.MAX_VALUE;
        for (AbstractAction a : possibleActions) {
            // Apply the action to the state
            AbstractGameState copyState = gameState.copy(playerId);
            getForwardModel().next(copyState, a);
            // ASK here we use state AFTER agents turn, later in qLearning
            // we use game state BEFORE agents turn, so they never match up
            // how to fix?
            double val = evaluate(copyState, playerId);
            if (val > maxValue) {
                maxActions = new LinkedList<AbstractAction>();
                maxActions.add(a);
                maxValue = val;
            } else if (val == maxValue)
                maxActions.add(a);
        }
        // Choose a random action that maximizes Q
        return maxActions.get(rng.nextInt(maxActions.size()));
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        if (pId == -1)
            pId = gameState.getCurrentPlayer();
        gameState = gameState.copy(pId);

        AbstractAction chosenAction = rng.nextFloat() > params.epsilon
                ? randArgmaxEvaluation(gameState, possibleActions, pId)
                : possibleActions.get(rng.nextInt(possibleActions.size()));

        // TODO implement better methods for reward (score, etc.?)
        float reward = 0;
        // ASK do i have to make a copy of gameState here?
        // ASK think i should have all states there, not every second one (since 2
        // players)
        turns.add(new Turn(gameState, chosenAction, possibleActions, reward));
        return chosenAction;
    }

    @Override
    public TabRLPlayer copy() {
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
