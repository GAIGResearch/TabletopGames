package players;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.observations.IObservation;

import java.util.List;
import java.util.Random;

public class RandomPlayer extends AbstractPlayer {

    /**
     * Random generator for this agent.
     */
    private final Random rnd;

    public RandomPlayer(Random rnd)
    {
        this.rnd = rnd;
    }

    public RandomPlayer()
    {
        this(new Random());
    }

    @Override
    public void initializePlayer(IObservation observation) {}

    @Override
    public void finalizePlayer(IObservation observation) {}

    @Override
    public int getAction(IObservation observation, List<AbstractAction> actions) {
        return rnd.nextInt(actions.size());
    }

    @Override
    public void registerUpdatedObservation(IObservation observation) {
        // Nothing to be done here, since the RandomPlayer does not need to react on such an observation.
    }
}
