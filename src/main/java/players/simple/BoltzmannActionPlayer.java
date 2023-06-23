package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.Utils;
import core.interfaces.IActionHeuristic;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

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

    final double temperature;
    final double epsilon;
    final Random rnd;

    final IActionHeuristic actionHeuristic;

    /**
     *
     * @param actionHeuristic An object that provides a value for any given action (given a context state)
     * @param temperature The temperature parameter for the Boltzmann distribution. Higher = more random; must be positive
     * @param epsilon The exploration parameter. Probability of selecting a random action.
     * @param seed Random seed
     */
    public BoltzmannActionPlayer(IActionHeuristic actionHeuristic, double temperature, double epsilon, long seed) {
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
        Function<AbstractAction, Double> valueFn = action -> actionHeuristic.evaluateAction(action, gameState);
        Map<AbstractAction, Double> actionToValueMap = possibleActions.stream().collect(toMap(Function.identity(), valueFn));
        return Utils.sampleFrom(actionToValueMap, temperature, epsilon, rnd);
    }

    @Override
    public AbstractPlayer copy() {
        return this; // stateless (except for rnd)
    }
}
