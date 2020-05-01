package players;

import core.actions.IAction;
import core.observations.Observation;

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
    public void initializePlayer(Observation observation) {}

    @Override
    public void finalizePlayer(Observation observation) {}

    @Override
    public int getAction(Observation observation, List<IAction> actions) {
        return rnd.nextInt(actions.size());
    }
}
