package players.rmhc;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

public class Individual implements Comparable {

    AbstractAction[] actions;         // Actions in individual. Intended max length of individual = actions.length
    AbstractGameState[] gameStates;   // Game states in individual.
    double value;                     // Fitness of individual, to be maximised.
    int length;                       // Actual length of individual, <= actions.length

    private Random gen;               // Random generator

    Individual(int L, AbstractForwardModel fm, AbstractGameState gs, int playerID, Random gen) {
        // Initialize
        this.gen = gen;
        actions = new AbstractAction[L];
        gameStates = new AbstractGameState[L+1];
        gameStates[0] = gs.copy();

        // Rollout with random actions and assign fitness value
        this.value = rollout(gs, fm, 0, L, playerID);
    }

    // Copy constructor
    Individual(Individual I){
        actions = new AbstractAction[I.actions.length];
        gameStates = new AbstractGameState[I.gameStates.length];
        length = I.length;

        for (int i = 0; i < length; i++){
            actions[i] = I.actions[i].copy();
            gameStates[i] = I.gameStates[i].copy();
        }

        value = I.value;
        gen = I.gen;
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
                gen.nextInt(length - 1);
            }
            // Last index is maximum intended individual length
            int endIndex = actions.length;
            // Game state to start from
            AbstractGameState gs = gameStates[startIndex];
            // Perform rollout and assign new value
            this.value = rollout(gs, fm, startIndex, endIndex, playerID);

            return length - startIndex;
        }
        return 0;
    }

    /**
     * Performs a rollout with random actions from startIndex to endIndex in the individual, from root game state gs.
     * Evaluates the final state reached and returns this value.
     * @param gs - root game state from which to start rollout
     * @param fm - forward model
     * @param startIndex - index in individual from which to start rollout
     * @param endIndex - index in individual where to end rollout
     * @param playerID - ID of player, used in state evaluation
     * @return - value of final state reached after rollout.
     */
    private double rollout(AbstractGameState gs, AbstractForwardModel fm, int startIndex, int endIndex, int playerID) {
        length = 0;
        for (int i = startIndex; i < endIndex; i++){
            // Rolls from chosen index to the end, randomly changing actions and game states
            // Length of individual is updated depending on if it reaches a terminal game state
            if (gs.isNotTerminal()) {
                // Copy the game state
                AbstractGameState gsCopy = gs.copy();
                List<AbstractAction> currentActions = gsCopy.getActions();

                // Advance game state with random action
                actions[i] = currentActions.get(gen.nextInt(currentActions.size()));
                fm.next(gsCopy, actions[i]);
                // Compute available actions and store this state
                fm.computeAvailableActions(gsCopy);
                gameStates[i + 1] = gsCopy;
                gs = gsCopy;
                // Individual length increased
                length++;
            } else {
                break;
            }
        }
        return gs.getScore(playerID);
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
