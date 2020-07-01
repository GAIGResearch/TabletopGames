package players;

import core.actions.AbstractAction;
import core.AbstractPlayer;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Random;

public class OSLA extends AbstractPlayer {

    private Random random; // random generator for noise
    private IStateHeuristic stateHeuristic;
    public double epsilon = 1e-6;

    public OSLA(){
        this.random = new Random();
    }

    public OSLA(Random random)
    {
        this.random = random;
    }

    @Override
    public AbstractAction getAction(AbstractGameState gs ) {
//        stateHeuristic = new PandemicDiffHeuristic((PandemicGameState)observation);

        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;
        List<AbstractAction> actions = gs.getActions();

        for (AbstractAction action : actions) {
            AbstractGameState gsCopy = gs.copy();
            getForwardModel().next(gsCopy, action);
            double valState = gsCopy.getScore(this.getPlayerID()); //stateHeuristic.evaluateState((AbstractGameState)gsCopy);

            double Q = noise(valState, this.epsilon, this.random.nextDouble());
//            System.out.println(valState);

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = action;
            }
        }
//        System.out.println();

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
