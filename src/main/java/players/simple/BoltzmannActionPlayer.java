package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import players.mcts.IMASTUser;
import utilities.Pair;
import utilities.Utils;

import java.util.*;

/**
 * This implementation of AbstractPlayer uses a Boltzmann distribution to select actions.
 * It requires:
 *  - an IActionHeuristic to evaluate the actions
 *  - a temperature parameter to control the randomness of the distribution. Higher temperature means more randomness.
 *  - an epsilon parameter to control the probability of selecting a random action
 *
 *  Hence, it can be used to implement an epsilon-greedy strategy by setting temperature to
 *  a large negative number and epsilon to a value > 0.
 */
public class BoltzmannActionPlayer extends AbstractPlayer implements IMASTUser {

    final public double temperature;
    final public double epsilon;

    protected IActionHeuristic actionHeuristic;

    /**
     *
     * @param actionHeuristic An object that provides a value for any given action (given a context state)
     * @param temperature The temperature parameter for the Boltzmann distribution. Higher = more random; must be positive
     * @param epsilon The exploration parameter. Probability of selecting a random action.
     * @param seed Random seed
     */
    public BoltzmannActionPlayer(IActionHeuristic actionHeuristic, double temperature, double epsilon, long seed) {
        super(null, "BoltzmannActionPlayer");
        this.actionHeuristic = actionHeuristic;
        this.temperature = Math.max(temperature, 0.001);
        this.epsilon = epsilon;
        this.rnd = new Random(seed);
    }

    public BoltzmannActionPlayer(IActionHeuristic actionHeuristic, double temperature, double epsilon) {
        this(actionHeuristic, temperature, epsilon, System.currentTimeMillis());
    }

    public BoltzmannActionPlayer(IActionHeuristic actionHeuristic) {
        this(actionHeuristic, 1.0, 0.0, System.currentTimeMillis());
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        double[] actionPDF = createPDF(gameState, possibleActions);
        if (epsilon > 0.0 && rnd.nextDouble() < epsilon)
            return possibleActions.get(rnd.nextInt(possibleActions.size()));
        return possibleActions.get(Utils.sampleFrom(actionPDF, rnd.nextDouble()));
    }

    private double[] createPDF(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        double[] actionValues = actionHeuristic.evaluateAllActions(possibleActions, gameState);
        double[] boltzmann = Utils.exponentiatePotentials(actionValues, temperature);
        return Utils.pdf(boltzmann);
    }

    // for testing
    public double valueOf(AbstractAction action, AbstractGameState gameState) {
        return actionHeuristic.evaluateAction(action, gameState, new ArrayList<>());
    }

    // for testing
    public double probabilityOf(AbstractAction action, AbstractGameState gameState, List<AbstractAction> allActions) {
        double[] actionValues = actionHeuristic.evaluateAllActions(allActions, gameState);
        double sum = 0;
        for (double value : actionValues) {
            sum += Math.exp(value / temperature);
        }
        return Math.exp(actionHeuristic.evaluateAction(action, gameState, new ArrayList<>()) / temperature) / sum;
    }

    @Override
    public AbstractPlayer copy() {
        return this; // stateless (except for rnd)
    }

    public IActionHeuristic getActionHeuristic() {
        return actionHeuristic;
    }

    public void setActionHeuristic(IActionHeuristic actionHeuristic) {
        this.actionHeuristic = actionHeuristic;
    }

    @Override
    public void setMASTStats(List<Map<Object, Pair<Integer, Double>>> MASTStats) {
        if (actionHeuristic instanceof IMASTUser) {
            ((IMASTUser) actionHeuristic).setMASTStats(MASTStats);
        }
    }
}
