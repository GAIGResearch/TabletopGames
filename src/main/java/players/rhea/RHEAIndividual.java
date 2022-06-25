package players.rhea;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import org.graalvm.compiler.core.common.type.ArithmeticOpTable;

import java.util.List;
import java.util.Random;

public class RHEAIndividual implements Comparable<RHEAIndividual> {

    AbstractAction[] actions;         // Actions in individual. Intended max length of individual = actions.length
    AbstractGameState[] gameStates;   // Game states in individual.
    double value;                     // Fitness of individual, to be maximised.
    int length;                       // Actual length of individual, <= actions.length
    double discountFactor;            // Discount factor for calculating rewards
    IStateHeuristic heuristic;
    private Random gen;               // Random generator

    RHEAIndividual(int L, double discountFactor, AbstractForwardModel fm, AbstractGameState gs, int playerID, Random gen, IStateHeuristic heuristic) {
        // Initialize
        this.gen = gen;
        this.discountFactor = discountFactor;
        actions = new AbstractAction[L];
        gameStates = new AbstractGameState[L + 1];
        this.heuristic = heuristic;

        // Rollout with random actions and assign fitness value
        gameStates[0] = gs.copy();
        rollout(fm, 0, playerID, true);  // TODO: cheating, init should also count FM calls
    }

    // Copy constructor
    RHEAIndividual(RHEAIndividual I) {
        actions = new AbstractAction[I.actions.length];
        gameStates = new AbstractGameState[I.gameStates.length];
        length = I.length;
        discountFactor = I.discountFactor;
        heuristic = I.heuristic;

        for (int i = 0; i < length; i++) {
            actions[i] = I.actions[i]; //.copy();
            gameStates[i] = I.gameStates[i]; //.copy(); // Should not need to copy game states, as we always copy before we use!
        }

        value = I.value;
        gen = I.gen;
    }

    /**
     * Mutates this individual, by picking an index and changing all genes from that point on.
     * Updates the length of the individual in case the rollout hits game end.
     * Also evaluates the individual as a rollout is needed for mutation, and updates the value.
     *
     * @param fm       - forward model
     * @param playerID - ID of player, used in evaluation of fitness
     * @return number of calls to the FM.next() function, as the difference between length after rollout and start
     * index of rollout
     */
    public int mutate(AbstractForwardModel fm, int playerID, int mutationCount) {
            // Find index from which to mutate individual, random in range of currently valid length
            int startIndex = actions.length;
            for (int mutation = 0; mutation < mutationCount; mutation++) {
                int position = gen.nextInt(actions.length);
                List<AbstractAction> available = fm.computeAvailableActions(gameStates[position]);
                actions[position] = available.get(gen.nextInt(available.size()));
                if (position < startIndex)
                    startIndex = position;  // start the rollout from the first mutation
            }

            // Perform rollout and return number of FM calls taken.
            return rollout(fm, startIndex, playerID, true);
    }

    /**
     * Performs a rollout with random actions from startIndex to endIndex in the individual, from root game state gs.
     * Starts by repairing the full individual, then mutates it, and finally evaluates it.
     * Evaluates the final state reached and returns the number of calls to the FM.next() function.
     *
     * @param fm         - forward model
     * @param startIndex - index in individual from which to start rollout
     * @param playerID   - ID of player, used in state evaluation
     * @return - number of calls to the FM.next() function
     */
    public int rollout(AbstractForwardModel fm, int startIndex, int playerID, boolean repair) {
        length = 0;
        double delta = 0;
        double previousScore = 0;
        int fmCalls = 0;
        AbstractGameState gs = gameStates[startIndex].copy();

        for (int i = 0; i < startIndex; i++) {
            double score;
            if (this.heuristic != null) {
                score = heuristic.evaluateState(gameStates[i + 1], playerID);
            } else {
                score = gameStates[i + 1].getGameScore(playerID);
            }
            delta += Math.pow(discountFactor, i) * (score - previousScore);
            previousScore = score;
        }

        for (int i = startIndex; i < actions.length; i++) {
            // Rolls from chosen index to the end, randomly changing actions and game states
            // Length of individual is updated depending on if it reaches a terminal game state
            if (gs.isNotTerminal()) {
                // is the action valid
                AbstractAction action;
                AbstractGameState gsCopy = gs.copy();
                List<AbstractAction> currentActions = fm.computeAvailableActions(gsCopy);
                boolean illegalAction = actions[i] == null || !currentActions.contains(actions[i]);
                if (illegalAction) {
                    action = currentActions.get(gen.nextInt(currentActions.size()));
                    if (repair) // if we are repairing then we override an illegal action with a random legitimate one
                        actions[i] = action;
                } else {
                    action = actions[i];
                }
                // TODO: Add a closed loop option to not copy the state (expensively) if the action is valid, but jump to the next state stored
                fm.next(gsCopy, action.copy());
                fmCalls++;

                // If it's my turn, store this in the individual
                while (gsCopy.isNotTerminal() && !(gsCopy.getCurrentPlayer() == playerID)) {
                    // now we fast forward through any opponent moves with a random OM
                    // TODO: Add in other opponent model options
                    List<AbstractAction> moves = fm.computeAvailableActions(gsCopy);
                    fm.next(gsCopy, moves.get(gen.nextInt(moves.size())));
                }
                gameStates[i + 1] = gsCopy;
                // Individual length increased
                length++;

                // Add value of state, discounted
                double score;
                if (this.heuristic != null) {
                    score = heuristic.evaluateState(gameStates[i + 1], playerID);
                } else {
                    score = gameStates[i + 1].getGameScore(playerID);
                }
                delta += Math.pow(discountFactor, i) * (score - previousScore);
                previousScore = score;

                gs = gsCopy;

            } else {
                break;
            }
        }
//        this.value = gs.getScore(playerID);
        this.value = delta;
        return fmCalls;
    }

    @Override
    public int compareTo(RHEAIndividual b) {
        RHEAIndividual a = this;
        return Double.compare(b.value, a.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RHEAIndividual)) return false;

        RHEAIndividual a = this;
        RHEAIndividual b = (RHEAIndividual) o;

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
