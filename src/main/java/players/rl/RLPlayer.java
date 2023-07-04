package players.rl;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.rl.utils.ApplyActionStateFeatureVector;


public class RLPlayer extends AbstractPlayer {

    public static final String resourcesPath = "src/main/java/players/rl/resources/";

    public enum RLType {
        Tabular(QWDSTabular.class),
        LinearApprox(QWDSLinearApprox.class);

        final Class<? extends QWeightsDataStructure> qWeightClass;

        RLType(Class<? extends QWeightsDataStructure> qWeightClass) {
            this.qWeightClass = qWeightClass;
        }
    }

    private Random rng;

    final public RLParams params;
    private QWeightsDataStructure qWeights;

    private RLTrainer trainer;

    public RLPlayer(RLParams params, String inFileNameOrAbsPath) {
        if (inFileNameOrAbsPath == null)
            throw new IllegalArgumentException("Must provide file name or absolute path for input file");
        this.params = params;
        this.qWeights = instantiateQWeights(inFileNameOrAbsPath);
        this.qWeights.setPlayerParams(params);
    }

    // Not public since only RLTrainer should be allowed (and need) to call this.
    // All non-training instances should use other constructor
    RLPlayer(RLParams params, QWeightsDataStructure qwds, RLTrainer trainer) {
        this.params = params;
        this.params.type = qwds.getType();
        this.qWeights = qwds;
        this.qWeights.setPlayerParams(params);
        this.trainer = trainer;
        this.qWeights.setTrainingParams(trainer.params);
    }

    private QWeightsDataStructure instantiateQWeights(String inFileNameOrAbsPath) {
        try {
            return params.type.qWeightClass.getDeclaredConstructor(String.class).newInstance(inFileNameOrAbsPath);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        this.rng = new Random(params.getRandomSeed());
        if (params.features instanceof ApplyActionStateFeatureVector)
            ((ApplyActionStateFeatureVector) this.params.features).linkPlayer(this);
        this.qWeights.initialize(gameState.getGameType().name());
    }

    @Override
    public void finalizePlayer(AbstractGameState gameState) {
        if (trainer == null)
            return;
        trainer.train(this, gameState);
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        AbstractAction chosenAction = trainer == null || rng.nextFloat() > params.epsilon
                ? randArgmaxEvaluation(gameState, possibleActions)
                : possibleActions.get(rng.nextInt(possibleActions.size()));

        // TODO implement better methods for reward (score, etc.?)
        if (trainer != null)
            trainer.addTurn(this, gameState, chosenAction, possibleActions);
        return chosenAction;
    }

    private AbstractAction randArgmaxEvaluation(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        // Choose an action that maximizes the Q-function
        List<AbstractAction> maximizingActions = new LinkedList<AbstractAction>();
        double qMax = Double.NEGATIVE_INFINITY;
        for (AbstractAction a : possibleActions) {
            // Apply the action to the state
            double q = qWeights.evaluateQ(this, gameState, a);
            // Keep all actions that maximize Q
            if (q > qMax) {
                maximizingActions.clear();
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
