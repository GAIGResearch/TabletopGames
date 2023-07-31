package players.rl;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.sushigo.SGGameState;
import players.rl.utils.ApplyActionStateFeatureVector;


public class RLPlayer extends AbstractPlayer {

    public static final String resourcesPathName = "src/main/java/players/rl/resources/";

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
    private QWeightsDataStructure qwds;

    private RLTrainer trainer;

    public RLPlayer(RLParams params) {
        this.params = params;
    }

    // Not public since only RLTrainer should be allowed (and need) to call this
    RLPlayer(RLParams params, QWeightsDataStructure qwds, RLTrainer trainer) {
        this(params);
        if (params.infileNameOrPath != null)
            this.params.initializeFromInfile(trainer.trainingParams.gameName);
        this.qwds = qwds;
        qwds.initialize(trainer.trainingParams, params);
        this.trainer = trainer;
    }

    private QWeightsDataStructure instantiateQWeights() {
        QWeightsDataStructure qwds = null;
        try {
            qwds = params.getType().qWeightClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return qwds;
    }

    @Override
    public void initializePlayer(AbstractGameState gameState) {
        initializePlayer(gameState.getGameType().name());
    }

    void initializePlayer(String gameName) {
        this.rng = new Random(params.getRandomSeed());

        if (params.infileNameOrPath != null)
            params.initializeFromInfile(gameName);
        if (params.getFeatures() instanceof ApplyActionStateFeatureVector)
            ((ApplyActionStateFeatureVector) params.getFeatures()).linkPlayer(this);

        if (qwds == null)
            qwds = instantiateQWeights();
        qwds.initialize(gameName, params);
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
            double q = qwds.evaluateQ(this, gameState, a);
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
