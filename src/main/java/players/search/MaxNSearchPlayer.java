package players.search;

import core.*;
import core.actions.AbstractAction;
import players.mcts.ActionStats;
import core.interfaces.IStateHeuristic;
import llm.IHasStateHeuristic;

import java.util.*;

public class MaxNSearchPlayer extends AbstractPlayer implements IHasStateHeuristic {
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
     * <p>
     * Additionally, the BUDGET can be specified as a cutoff for the search. If this much time passes
     * without the search finishing, the best action found so far is returned (likely to be pretty random).
     */


    private long startTime;
    private SearchResult rootResult;

    protected List<Map<AbstractAction, ActionStats>> actionValueEstimates;

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
        rootResult = null;
        actionValueEstimates = new ArrayList<>();
        if (getParameters().iterativeDeepening) {
            // we do a depth D = 1 search, then D = 2 and so on until we reach maxDepth or exhaust budget
            for (int depth = 1; depth <= getParameters().searchDepth; depth++) {
                if (depth == 1)
                    actionValueEstimates.add(new HashMap<>());
                else {
                    // initialise the actionValueEstimates for the next depth with the values from the previous depth
                    Map<AbstractAction, ActionStats> newMap = new HashMap<>();
                    for (Map.Entry<AbstractAction, ActionStats> entry : actionValueEstimates.get(0).entrySet()) {
                        newMap.put(entry.getKey(), entry.getValue().copy());
                    }
                    actionValueEstimates.add(0, newMap);
                }
                rootResult = expand(gs, actions, depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
            }
        } else {
            for (int depth = 0; depth < getParameters().searchDepth; depth++) {
                actionValueEstimates.add(new HashMap<>());
            }
            rootResult = expand(gs, actions, getParameters().searchDepth,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        }
        return rootResult == null ? null : rootResult.action;
    }

    public SearchResult getRootResult() {
        return rootResult;
    }

    @Override
    public void setStateHeuristic(IStateHeuristic heuristic) {
        getParameters().setParameterValue("heuristic", heuristic);
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return getParameters().heuristic;
    }

    /**
     * This returns a Pair.
     * The first element is the best action to take, based on the recursive search.
     * The second element is the value of the state on the assumption this best action is taken.
     */
    protected SearchResult expand(AbstractGameState state, List<AbstractAction> actions, int searchDepth,
                                  double alpha, double beta) {
        MaxNSearchParameters params = getParameters();
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
            return new SearchResult(null, values, alpha, beta, null);
        }

        // otherwise we recurse to find the best action and value
        double[] bestValues = new double[state.getNPlayers()];
        double bestValue = Double.NEGATIVE_INFINITY;
        AbstractAction bestAction = null;
        // we shuffle the actions so that ties are broken at random
        if (params.expandByEstimatedValue) {
            // sort actions based on actionValueEstimates (with highest value first)
            actions.sort(Comparator.comparingDouble(a -> -actionValueEstimates.get(searchDepth - 1)
                    .getOrDefault(a, new ActionStats(state.getNPlayers()))
                    .totValue[state.getCurrentPlayer()]));
        } else {
            Collections.shuffle(actions, getRnd());
        }
        Map<AbstractAction, ActionStats> statsMap = actionValueEstimates.get(searchDepth - 1);
        Map<AbstractAction, double[]> actionValues = new HashMap<>();
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
            SearchResult result = expand(stateCopy, nextActions, newDepth, alpha, beta);
            if (params.expandByEstimatedValue) {
                // we store the value estimates for each action
                if (!statsMap.containsKey(action)) {
                    statsMap.put(action, new ActionStats(state.getNPlayers()));
                }
                statsMap.get(action).update(result.value);
            }

            actionValues.put(action, result.value);
            // we make the decision based on the actor at state, not the actor at stateCopy
            if (result.value[state.getCurrentPlayer()] > bestValue) {
                bestAction = action;
                bestValue = result.value[state.getCurrentPlayer()];
                bestValues = result.value;

                if (params.paranoid && params.alphaBetaPruning) {
                    // alpha-beta pruning
                    // bestValue is already from the perspective of the current player (i.e. negated for opponents)
                    if (getPlayerID() == state.getCurrentPlayer()) {
                        if (bestValue > beta) {
                            return new SearchResult(bestAction, bestValues, alpha, beta, actionValues);
                        }
                        alpha = Math.max(alpha, bestValue);
                    } else {
                        if (-bestValue < alpha) {
                            return new SearchResult(bestAction, bestValues, alpha, beta, actionValues);
                        }
                        beta = Math.min(beta, -bestValue);
                    }
                }
            }

            if (System.currentTimeMillis() - startTime > params.budget) {
                // out of time - return best action so far
                return new SearchResult(bestAction, bestValues, alpha, beta, actionValues);
            }
        }
        if (bestAction == null) {
            throw new AssertionError("No best action found");
        }
        return new SearchResult(bestAction, bestValues, alpha, beta, actionValues);
    }

    @Override
    public MaxNSearchPlayer copy() {
        MaxNSearchPlayer retValue = new MaxNSearchPlayer((MaxNSearchParameters) getParameters().shallowCopy());
        if (getForwardModel() != null)
            retValue.setForwardModel(getForwardModel());
        return retValue;
    }

    protected record SearchResult(AbstractAction action, double[] value, double alpha, double beta, Map<AbstractAction, double[]> allActionValues) {
    }

}
