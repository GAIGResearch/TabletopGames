package players;

import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.AbstractPlayer;
import core.AbstractGameState;
import core.interfaces.IObservation;
import games.pandemic.PandemicGameState;
import players.heuristics.PandemicHeuristic;
import players.heuristics.StateHeuristic;

import java.util.List;
import java.util.Random;

public class OSLA extends AbstractPlayer {

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
    public int getAction(AbstractGameState observation ) {
        stateHeuristic = new PandemicHeuristic((PandemicGameState)observation);
        PandemicGameState gs = (PandemicGameState)observation;

        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;
        List<AbstractAction> actions = gs.getActions();

        for (AbstractAction action : actions) {
            AbstractGameState gsCopy = gs.copy();
            getForwardModel().next(gsCopy, action);
            double valState = stateHeuristic.evaluateState((AbstractGameState)gsCopy);

            double Q = noise(valState, this.epsilon, this.random.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = action;
            }

        }

        return actions.indexOf(bestAction);
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
