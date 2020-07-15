package evaluation.testplayers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

import static utilities.Utils.noise;

public class OSLATestPlayer extends AbstractPlayer {

    private Random random; // random generator for noise
    public double epsilon = 1e-6;

    public OSLATestPlayer(){
        this.random = new Random();
    }

    public OSLATestPlayer(Random random)
    {
        this.random = random;
    }

    @Override
    public AbstractAction getAction(AbstractGameState gs ) {
        double maxQ = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;
        List<AbstractAction> actions = gs.getActions();

        for (AbstractAction action : actions) {
            AbstractGameState gsCopy = gs.copy();
            getForwardModel().next(gsCopy, action);
            double valState = gsCopy.getScore(this.getPlayerID());
            double Q = noise(valState, this.epsilon, this.random.nextDouble());

            if (Q > maxQ) {
                maxQ = Q;
                bestAction = action;
            }
        }

        return bestAction;
    }

}
