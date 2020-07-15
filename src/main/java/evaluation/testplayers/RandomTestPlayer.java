package evaluation.testplayers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.StatSummary;

import java.util.List;
import java.util.Random;

public class RandomTestPlayer extends AbstractPlayer {

    // Random generator for this agent.
    private final Random rnd;
    // Scores observed during the game
    private StatSummary scores;

    public RandomTestPlayer(Random rnd)
    {
        this.rnd = rnd;
        scores = new StatSummary();
    }

    public RandomTestPlayer()
    {
        this(new Random());
    }

    @Override
    public AbstractAction getAction(AbstractGameState observation) {
        int randomAction = rnd.nextInt(observation.getActions().size());
        List<AbstractAction> actions = observation.getActions();

        scores.add(observation.getScore(getPlayerID()));

        return actions.get(randomAction);
    }

    public StatSummary getScores() {
        return scores;
    }
}
