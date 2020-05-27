package players;

import core.AbstractGameState;
import core.AbstractPlayer;
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
    public int getAction(AbstractGameState observation) {
        return rnd.nextInt(observation.getActions().size());
    }
}
