package players.search;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import core.turnorders.StandardTurnOrder;
import players.PlayerParameters;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static utilities.Utils.noise;

public class SearchPlayer extends AbstractPlayer {


    private long startTime;
    private boolean outOfTime = false;
    public SearchPlayer(SearchParameters parameters) {
        super(parameters, "MinMaxSearch");
    }

    @Override
    public SearchParameters getParameters() {
        return (SearchParameters) this.parameters;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
        // For each action we copy the state and recursively call the expand method
        // depending on the search unit we decrement the search depth on the recursive call:
        // - ACTION: always
        // - MACRO_ACTION: only when the currentPlayer() has changed as a result of applying the action
        // - TURN: only when turn number has changed as a result of applying the action
        outOfTime = false;
        startTime = System.currentTimeMillis();
        return expand(gs, actions, getParameters().searchDepth).a;
    }

    /**
     * This returns a Pair.
     * The first element is the best action to take, based on the recursive search.
     * The second element is the value of the state on the assumption this best action is taken.
     */
    protected Pair<AbstractAction, Double[]> expand(AbstractGameState state, List<AbstractAction> actions, int searchDepth) {
        SearchParameters params = getParameters();
        if (System.currentTimeMillis() - startTime > params.budget) {
            // out of time - return null action and a vector of zeros
            Double[] allZeros = new Double[state.getNPlayers()];
            Arrays.fill(allZeros, 0.0);
            return new Pair<>(null, allZeros);
        }
        // if we have reached the end of the search, or the state is terminal, we evaluate the state
        if (searchDepth == 0 || !state.isNotTerminal()) {
            // when valuing a state, we need to record the full vector of values for each player
            // as all of these need to be back-propagated up so that the relevant one can be used for decision-making
            // if paranoid and this action belongs to another player, we assume they try to minimise our score
            Double[] values = new Double[state.getNPlayers()];
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
            return new Pair<>(null, values);
        }

        // otherwise we recurse to find the best action and value
        double bestValue = Double.NEGATIVE_INFINITY;
        Double[] bestValues = new Double[state.getNPlayers()];
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
            Pair<AbstractAction, Double[]> option = expand(stateCopy, nextActions, newDepth);
            if (outOfTime) {
                return new Pair<>(null, bestValues);
            }

            // we make the decision based on the actor at state, not the actor at stateCopy
            if (option.b[state.getCurrentPlayer()] > bestValue) {
                bestAction = action;
                bestValues = option.b;
                bestValue = bestValues[state.getCurrentPlayer()];
            }
        }
        if (bestAction == null) {
            throw new AssertionError("No best action found");
        }
        return new Pair<>(bestAction, bestValues);
    }

    @Override
    public SearchPlayer copy() {
        SearchPlayer retValue = new SearchPlayer((SearchParameters) getParameters().shallowCopy());
        retValue.setForwardModel(getForwardModel().copy());
        return retValue;
    }

}
