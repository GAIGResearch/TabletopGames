package players.simple;

import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.AbstractPlayer;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Random;

import static utilities.Utils.noise;

public class OSLAPlayer extends AbstractPlayer {

    private Random random; // random generator for noise
    public double epsilon = 1e-6;
    // Heuristics used for the agent
    IStateHeuristic heuristic;

    public OSLAPlayer(Random random)  {
        this.random = random;
    }

    public OSLAPlayer(){
        this(new Random());
    }

    public OSLAPlayer(IStateHeuristic heuristic){
        this(heuristic, new Random());
    }

    public OSLAPlayer(IStateHeuristic heuristic, Random random){
        this(random);
        this.heuristic = heuristic;
    }

    @Override
    public AbstractAction getAction(AbstractGameState gs, List<AbstractAction> actions) {

        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;

        for (AbstractAction action : actions) {
            AbstractGameState gsCopy = gs.copy();
            getForwardModel().next(gsCopy, action);
            double valState = 0;
            if (heuristic != null){
                valState = heuristic.evaluateState(gsCopy, this.getPlayerID());
            } else {
                valState = gsCopy.getScore(this.getPlayerID());
            }

            double Q = noise(valState, this.epsilon, this.random.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = action;
            }
        }

        return bestAction;
    }

    @Override
    public String toString() {
        return "OSLA";
    }

}
