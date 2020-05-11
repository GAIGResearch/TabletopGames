package players;

import actions.Action;
import core.AIPlayer;
import core.GameState;
import players.heuristics.PandemicHeuristic;
import players.heuristics.StateHeuristic;

import java.util.List;
import java.util.Random;

public class OSLA implements AIPlayer {

    private Random random; // random generator for noise
    private StateHeuristic stateHeuristic;
    public double epsilon = 1e-6;

    public OSLA(){
        this.random = new Random();
    }

    public OSLA(Random random)
    {
        this.random = random;
    }

    @Override
    public Action getAction(GameState gameState) {
        List<Action> actions = gameState.possibleActions();

        stateHeuristic = new PandemicHeuristic(gameState);

        double maxQ = Double.NEGATIVE_INFINITY;
        Action bestAction = null;

        for (Action action : actions) {
            GameState gsCopy = gameState.copy();

            gsCopy.next(action);
            double valState = stateHeuristic.evaluateState(gsCopy);

            double Q = noise(valState, this.epsilon, this.random.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = action;
            }

        }

        return bestAction;
    }

    public static double noise(double input, double epsilon, double random)
    {
        if(input != -epsilon) {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }else {
            return (input + epsilon) * (1.0 + epsilon * (random - 0.5));
        }
    }
}
