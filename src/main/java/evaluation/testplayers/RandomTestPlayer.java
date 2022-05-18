package evaluation.testplayers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.TAGStatSummary;

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
    private TAGStatSummary bf;
    // Scores observed during the game
    private TAGStatSummary scores;

    public RandomTestPlayer() {
        this(new Random());
    }

    public RandomTestPlayer(Random random) {
        this.random = random;
        scores = new TAGStatSummary();
        bf = new TAGStatSummary();
    }

    @Override
    public AbstractAction getAction(AbstractGameState gs, List<AbstractAction> actions ) {
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

    public TAGStatSummary getBranchingFactor() {
        return bf;
    }

    public TAGStatSummary getScores() {
        return scores;
    }
}
