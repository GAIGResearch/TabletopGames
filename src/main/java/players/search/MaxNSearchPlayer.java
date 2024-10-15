package players.search;

import core.*;
import core.actions.AbstractAction;

import java.util.Collections;
import java.util.List;

public class MaxNSearchPlayer extends AbstractPlayer {
    /**
     * This class is a simple implementation of a MaxN search player.
     * (This is the same as Minimax in the case of 2 players if the heuristic is symmetric)
     * <p></p>
     * This conducts a depth-first search of the game tree to a maximum depth of D.
     * A vector of heuristic values is back-propagated up the tree, with one entry for each player.
     * The assumption is that at each decision node, the acting player takes the action that
     * maximises their own result.
     * <p></p>
     * The key parameters the algorithm takes are:
     * - searchDepth: the maximum depth of the search (D)
     * - heuristic: the heuristic function to evaluate the state of the game
     * - paranoid: if true, the algorithm assumes that all players are trying to minimise its score
     * - searchUnit: the unit of search (ACTION, MACRO_ACTION, TURN)
     * <p></p>
     * The searchUnit determines how we measure D, or searchDepth.
     * - ACTION: D is decremented at each decision node
     * - MACRO_ACTION: D is decremented at each decision node where the acting player changes
     * - TURN: D is decremented at each decision node where the turn number changes
     *
     * Additionally, the BUDGET can be specified as a cutoff for the search. If this much time passes
     * without the search finishing, the best action found so far is returned (likely to be pretty random).
     */


    private long startTime;
    public MaxNSearchPlayer(MaxNSearchParameters parameters) {
        super(parameters, "MinMaxSearch");
    }

    @Override
    public MaxNSearchParameters getParameters() {
        return (MaxNSearchParameters) this.parameters;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
        // For each action we copy the state and recursively call the expand method
        // depending on the search unit we decrement the search depth on the recursive call:
        // - ACTION: always
        // - MACRO_ACTION: only when the currentPlayer() has changed as a result of applying the action
        // - TURN: only when turn number has changed as a result of applying the action
        startTime = System.currentTimeMillis();
        return expand(gs, actions, getParameters().searchDepth).action;
    }

    /**
     * This returns a Pair.
     * The first element is the best action to take, based on the recursive search.
     * The second element is the value of the state on the assumption this best action is taken.
     */
    protected SearchResult expand(AbstractGameState state, List<AbstractAction> actions, int searchDepth) {
        MaxNSearchParameters params = getParameters();
        if (System.currentTimeMillis() - startTime > params.budget) {
            // out of time - return null action and a vector of zeros
            return new SearchResult(null, new double[state.getNPlayers()]);
        }
        // if we have reached the end of the search, or the state is terminal, we evaluate the state
        if (searchDepth == 0 || !state.isNotTerminal()) {
            // when valuing a state, we need to record the full vector of values for each player
            // as all of these need to be back-propagated up so that the relevant one can be used for decision-making
            // if paranoid and this action belongs to another player, we assume they try to minimise our score
            double[] values = new double[state.getNPlayers()];
            if (params.paranoid) {
                double value = params.heuristic.evaluateState(state, getPlayerID());
                for (int i = 0; i < state.getNPlayers(); i++) {
                    values[i] = i == getPlayerID() ? value : -value;
                }
            } else {
                for (int i = 0; i < state.getNPlayers(); i++) {
                    values[i] = params.heuristic.evaluateState(state, i);
                }
            }
            return new SearchResult(null, values);
        }

        // otherwise we recurse to find the best action and value
        double bestValue = Double.NEGATIVE_INFINITY;
        double[] bestValues = new double[state.getNPlayers()];
        AbstractAction bestAction = null;
        // we shuffle the actions so that ties are broken at random
        Collections.shuffle(actions, getRnd());
        for (AbstractAction action : actions) {
            AbstractGameState stateCopy = state.copy();
            getForwardModel().next(stateCopy, action);
            // if we are at the bottom, then save a bit of time by not calculating the valid actions (which we'll never try)
            List<AbstractAction> nextActions = searchDepth > 0 ? getForwardModel().computeAvailableActions(stateCopy) : List.of();

            int newDepth = switch (params.searchUnit) {
                case ACTION -> searchDepth - 1;
                case MACRO_ACTION ->
                        state.getCurrentPlayer() != stateCopy.getCurrentPlayer() ? searchDepth - 1 : searchDepth;
                case TURN -> state.getTurnCounter() != stateCopy.getTurnCounter() ? searchDepth - 1 : searchDepth;
            };

            // recurse - we are here just interested in the value of stateCopy, and hence of taking action
            // We are not interested in the best action from stateCopy
            SearchResult result = expand(stateCopy, nextActions, newDepth);

            // we make the decision based on the actor at state, not the actor at stateCopy
            if (result.value[state.getCurrentPlayer()]  > bestValue) {
                bestAction = action;
                bestValues = result.value;
                bestValue = bestValues[state.getCurrentPlayer()];
            }
        }
        if (bestAction == null) {
            throw new AssertionError("No best action found");
        }
        return new SearchResult(bestAction, bestValues);
    }

    @Override
    public MaxNSearchPlayer copy() {
        MaxNSearchPlayer retValue = new MaxNSearchPlayer((MaxNSearchParameters) getParameters().shallowCopy());
        retValue.setForwardModel(getForwardModel().copy());
        return retValue;
    }

    protected record SearchResult(AbstractAction action, double[] value) {
    }

}
