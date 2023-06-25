package players.rl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.CoreConstants.GameResult;
import core.actions.AbstractAction;
import players.rl.dataStructures.QWeightsDataStructure;
import players.rl.dataStructures.TurnSAR;


public class RLPlayer extends AbstractPlayer {

    enum RLType {
        TABULAR,
        LINEAR_APPROX,
    }

    protected final Random rng;
    protected final String resourcesPath = "src/main/java/players/rl/resources/";

    public RLTrainer trainer;

    final public RLParams params;

    private QWeightsDataStructure qWeights;

    public RLPlayer(QWeightsDataStructure qWeights, RLParams params) {
        this.rng = new Random(params.getRandomSeed());
        this.params = params;
        this.qWeights = qWeights;
        this.qWeights.setParams(params);
    }

    protected RLPlayer(QWeightsDataStructure qWeights, RLParams params, RLTrainer trainer) {
        this(qWeights, params);
        this.trainer = trainer;
        this.qWeights.setTrainingParams(trainer.params);
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        this.params.features.linkPlayer(this);

        if (trainer == null) {
            String readPath = resourcesPath + gameState.getGameType().name() + "/beta.txt";
            Path path = Paths.get(readPath);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                try {
                    String[] qWeightStrings = Files.readString(path).split("\n");
                    qWeights.parseQWeightsTextFile(qWeightStrings);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void finalizePlayer(AbstractGameState gameState) {
        if (trainer == null)
            return;
        float reward = gameState.getPlayerResults()[getPlayerID()] == GameResult.WIN_GAME ? 1 : -1;
        trainer.addTurn(getPlayerID(), new TurnSAR(gameState, null, null, reward));
        trainer.train(this);
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        AbstractAction chosenAction = trainer == null || rng.nextFloat() > params.epsilon
                ? randArgmaxEvaluation(gameState, possibleActions)
                : possibleActions.get(rng.nextInt(possibleActions.size()));

        // TODO implement better methods for reward (score, etc.?)
        if (trainer != null)
            trainer.addTurn(getPlayerID(), new TurnSAR(gameState, chosenAction, possibleActions, 0));
        return chosenAction;
    }

    protected AbstractAction randArgmaxEvaluation(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        // Choose an action that maximizes the Q-function
        List<AbstractAction> maximizingActions = new LinkedList<AbstractAction>();
        double qMax = -Double.MAX_VALUE;
        for (AbstractAction a : possibleActions) {
            // Apply the action to the state
            double q = qWeights.evaluateQ(this, gameState, a);
            // Keep all actions that maximize Q
            if (q > qMax) {
                maximizingActions = new LinkedList<AbstractAction>();
                maximizingActions.add(a);
                qMax = q;
            } else if (q == qMax)
                maximizingActions.add(a);
        }
        // Choose a random action that maximizes Q
        return maximizingActions.get(rng.nextInt(maximizingActions.size()));
    }

    @Override
    public RLPlayer copy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

}
