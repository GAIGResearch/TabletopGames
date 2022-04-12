package players.simple;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

public class RandomPlayer extends AbstractPlayer {

    /**
     * Random generator for this agent.
     */
    private final Random rnd;

    public RandomPlayer(Random rnd) {
        this.rnd = rnd;
    }

    public RandomPlayer()
    {
        this(new Random());
    }

    @Override
    public AbstractAction getAction(AbstractGameState observation, List<AbstractAction> actions) {
        int randomAction = rnd.nextInt(actions.size());
        return actions.get(randomAction);
    }

    @Override
    public String toString() {
        return "Random";
    }

    @Override
    public RandomPlayer copy() {
        return new RandomPlayer(new Random(rnd.nextInt()));
    }
}
