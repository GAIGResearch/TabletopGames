package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import utilities.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
public class BoltzmannActionPlayer extends AbstractPlayer {

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
        double[] actionValues = actionHeuristic.evaluateAllActions(possibleActions, gameState);
        Map<AbstractAction, Double> actionToValueMap = new HashMap<>();
        for (int i = 0; i < possibleActions.size(); i++) {
            actionToValueMap.put(possibleActions.get(i), actionValues[i]);
        }
        return Utils.sampleFrom(actionToValueMap, temperature, epsilon, rnd.nextDouble());
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
}
