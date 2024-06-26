package players.rmhc;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;

import java.util.List;
import java.util.Random;

public class Individual implements Comparable {

    AbstractAction[] actions;         // Actions in individual. Intended max length of individual = actions.length
    AbstractGameState[] gameStates;   // Game states in individual.
    double value;                     // Fitness of individual, to be maximised.
    int length;                       // Actual length of individual, <= actions.length
    double discountFactor;            // Discount factor for calculating rewards

    private Random gen;               // Random generator
    IStateHeuristic heuristic;

    Individual(int L, double discountFactor, AbstractForwardModel fm, AbstractGameState gs, int playerID, Random gen, IStateHeuristic heuristic) {
        // Initialize
        this.gen = gen;
        this.discountFactor = discountFactor;
        actions = new AbstractAction[L];
        gameStates = new AbstractGameState[L+1];
        gameStates[0] = gs.copy();
        this.heuristic = heuristic;

        // Rollout with random actions and assign fitness value
        rollout(gs, fm, 0, playerID);
    }

    // Copy constructor
    Individual(Individual I){
        actions = new AbstractAction[I.actions.length];
        gameStates = new AbstractGameState[I.gameStates.length];
        length = I.length;
        discountFactor = I.discountFactor;

        for (int i = 0; i < length; i++){
            actions[i] = I.actions[i].copy();
            gameStates[i] = I.gameStates[i].copy();
        }

        value = I.value;
        gen = I.gen;
        heuristic = I.heuristic;
    }

    /**
     * Mutates this individual, by picking an index and changing all genes from that point on.
     * Updates the length of the individual in case the rollout hits game end.
     * Also evaluates the individual as a rollout is needed for mutation, and updates the value.
     * @param fm - forward model
     * @param playerID - ID of player, used in evaluation of fitness
     * @return number of calls to the FM.next() function, as the difference between length after rollout and start
     *          index of rollout
     */
    public int mutate(AbstractForwardModel fm, int playerID){
        if (length > 0) {
            // Find index from which to mutate individual, random in range of currently valid length
            int startIndex = 0;
            if (length > 1) {
                startIndex = gen.nextInt(length - 1);
            }

            AbstractGameState gs = gameStates[startIndex];
            // Perform rollout and return number of FM calls taken
            return rollout(gs, fm, startIndex, playerID);
        }
        return 0;
    }

    /**
     * Performs a rollout with random actions from startIndex to endIndex in the individual, from root game state gs.
     * Evaluates the final state reached and returns the number of calls to the FM.next() function.
     * @param gs - root game state from which to start rollout
     * @param fm - forward model
     * @param startIndex - index in individual from which to start rollout
     * @param playerID - ID of player, used in state evaluation
     * @return - number of calls to the FM.next() function
     */
    private int rollout(AbstractGameState gs, AbstractForwardModel fm, int startIndex, int playerID) {
        length = 0;
        int fmCalls = 0;
        double delta = 0;
        double previousScore = 0;

        for (int i = 0; i < startIndex; i++) {
            double score;
            if (this.heuristic != null){
                score = heuristic.evaluateState(gameStates[i+1], playerID);
            } else {
                score = gameStates[i+1].getHeuristicScore(playerID);
            }
            if (Double.isNaN(score))
                throw new AssertionError("Illegal heuristic value - should be a number");
            delta += Math.pow(discountFactor, i) * (score - previousScore);
            previousScore = score;
        }

        for (int i = startIndex; i < actions.length; i++){
            // Rolls from chosen index to the end, randomly changing actions and game states
            // Length of individual is updated depending on if it reaches a terminal game state
            if (gs.isNotTerminal()) {
                // Copy the game state
                AbstractGameState gsCopy = gs.copy();
                List<AbstractAction> currentActions = fm.computeAvailableActions(gsCopy);
                AbstractAction action = null;
                if (currentActions.size() > 0) {
                    action = currentActions.get(gen.nextInt(currentActions.size()));
                }

                // Advance game state with random action
                fm.next(gsCopy, action);
                fmCalls ++;

                // If it's my turn, store this in the individual
                boolean iAmMoving = (gameStates[i].getCurrentPlayer() == playerID);
                if (iAmMoving) {
                    gameStates[i + 1] = gsCopy;
                    actions[i] = action;

                    // Individual length increased
                    length++;

                    // Add value of state, discounted
                    double score;
                    if (this.heuristic != null){
                        score = heuristic.evaluateState(gameStates[i+1], playerID);
                    } else {
                        score = gameStates[i+1].getHeuristicScore(playerID);
                    }
                    if (Double.isNaN(score))
                        throw new AssertionError("Illegal heuristic value - should be a number");
                    delta += Math.pow(discountFactor, i) * (score - previousScore);
                    previousScore = score;
                } else {
                    i--;
                }

                gs = gsCopy;
            } else {
                break;
            }
        }

        this.value = delta;
        return fmCalls;
    }

    @Override
    public int compareTo(Object o) {
        Individual a = this;
        Individual b = (Individual)o;
        return Double.compare(b.value, a.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Individual)) return false;

        Individual a = this;
        Individual b = (Individual)o;

        for (int i = 0; i < actions.length; i++) {
            if (!a.actions[i].equals(b.actions[i])) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("" + value + ": ");
        for (AbstractAction action : actions) s.append(action).append(" ");
        return s.toString();
    }
}
