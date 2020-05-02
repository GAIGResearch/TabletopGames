package players;

import core.actions.IAction;
import core.observations.IObservation;

import java.util.List;
import java.util.Random;

public class RandomPlayer extends AbstractPlayer {

    /**
     * Random generator for this agent.
     */
    private final Random rnd;

    public RandomPlayer(int playerID, Random rnd)
    {
        super(playerID);
        this.rnd = rnd;
    }

    public RandomPlayer(int playerID)
    {
        this(playerID, new Random());
    }

    @Override
    public void initializePlayer(IObservation observation) {}

    @Override
    public void finalizePlayer(IObservation observation) {}

    @Override
    public int getAction(IObservation observation, List<IAction> actions) {
        return rnd.nextInt(actions.size());
    }
}
