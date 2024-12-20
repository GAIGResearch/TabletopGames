package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

public class RandomPlayer extends AbstractPlayer {

    /**
     * Random generator for this agent.
     */
    public RandomPlayer(Random rnd) {
        super(null, "RandomPlayer");
        this.rnd = rnd;
    }

    public RandomPlayer()
    {
        this(new Random());
    }

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
        int randomAction = rnd.nextInt(actions.size());
        return actions.get(randomAction);
    }

    @Override
    public RandomPlayer copy() {
        RandomPlayer retValue = new RandomPlayer(new Random(rnd.nextInt()));
        retValue.decorators = decorators;
        retValue.setName(this.toString());
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RandomPlayer;
    }
}
