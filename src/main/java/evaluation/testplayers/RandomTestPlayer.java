package evaluation.testplayers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import evaluation.summarisers.TAGNumericStatSummary;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * Plays actions randomly, gathering statistics about:
 *  - branching factor
 *  - scores observed during the game
 */
public class RandomTestPlayer extends AbstractPlayer {

    // Random generator for action selection
    private Random random;
    // Record of branching factor
    private TAGNumericStatSummary bf;
    // Scores observed during the game
    private TAGNumericStatSummary scores;

    public RandomTestPlayer() {
        this(new Random());
    }

    public RandomTestPlayer(Random random) {
        super(null, "RandomTestPlayer");
        this.random = random;
        scores = new TAGNumericStatSummary();
        bf = new TAGNumericStatSummary();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
        // Iterate through all actions available to gather statistics
        HashSet<AbstractGameState> states = new HashSet<>();
        states.add(gs.copy());
        for (AbstractAction action : actions) {
            AbstractGameState gsCopy = gs.copy();
            getForwardModel().next(gsCopy, action);
            states.add(gsCopy);
            scores.add(gsCopy.getHeuristicScore(getPlayerID()));
        }
        bf.add(states.size());
        scores.add(gs.getHeuristicScore(getPlayerID()));

        return actions.get(random.nextInt(actions.size()));
    }

    @Override
    public RandomTestPlayer copy() {
        return this;
    }

    public TAGNumericStatSummary getBranchingFactor() {
        return bf;
    }

    public TAGNumericStatSummary getScores() {
        return scores;
    }
}
