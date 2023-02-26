package core;

import core.actions.AbstractAction;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class AbstractPlayer {

    protected IStatisticLogger statsLogger = null;
    // ID of this player, assigned by the game
    int playerID;
    String name;
    Random rnd = new Random(System.currentTimeMillis());
    // Forward model for the game
    private AbstractForwardModel forwardModel;
    private double exploreEpsilon;


    /* Final methods */

    /**
     * Retrieves this player's ID, as set by the game.
     *
     * @return - int, player ID
     */
    public final int getPlayerID() {
        return playerID;
    }

    /**
     * Retrieves the forward model for current game being played.
     *
     * @return - ForwardModel
     */
    public final AbstractForwardModel getForwardModel() {
        return forwardModel;
    }

    /**
     * Sets the forward model for the current environment.
     * This is used by Game, and also when an AbstractPlayer is a component of another agent
     * (for example in MCTSPlayer, where the heuristic rollout policy could itself be an AbstractPlayer
     * that needs a copy of the forward model)
     * <p>
     * Anything that has a reference to an AbstractPlayer can therefore pass a ForwardModel.
     * In competition mode this it is not currently possible for opponents to get a link to your
     * object...and this should be avoided.
     *
     * @param model
     */
    public void setForwardModel(AbstractForwardModel model) {
        this.forwardModel = model;
    }

    /**
     * This is the main method called by Game to get an Action. It implements an epsilon-Greedy wrapper around
     * of the main agent policy. In most cases exploreEpsilon will be zero; but this is useful where we
     * with to implement noise in the game - the main current example of this is in evaluation.ProgressiveLearner.
     * @param gameState
     * @param possibleActions
     * @return
     */
    public final AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        boolean explore = rnd.nextDouble() < exploreEpsilon;
        if (explore) {
            int roll = rnd.nextInt(possibleActions.size());
            return possibleActions.get(roll);
        } else {
            return _getAction(gameState, possibleActions);
        }
    }


    /* Methods that should be implemented in subclass */

    /**
     * Generate a valid action to play in the game. Valid actions can be found by accessing
     * AbstractGameState.getActions()
     *
     * @param gameState observation of the current game state
     */
    public abstract AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions);

    /* Methods that can be implemented in subclass */

    /**
     * Initialize agent given an observation of the initial game state.
     * e.g. load weights, initialize neural network
     *
     * @param gameState observation of the initial game state
     */
    public void initializePlayer(AbstractGameState gameState) {
    }

    /**
     * Finalize agent given an observation of the final game state.
     * e.g. store variables after training, modify weights, etc.
     *
     * @param gameState observation of the final game state
     */
    public void finalizePlayer(AbstractGameState gameState) {
    }

    /**
     * Receive an updated game state for which it is not required to respond with an action.
     *
     * @param gameState observation of the current game state
     */
    public void registerUpdatedObservation(AbstractGameState gameState) {
    }

    public final void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        if (name != null) return name;
        return this.getClass().getSimpleName();
    }

    public final IStatisticLogger getStatsLogger() {
        return statsLogger;
    }

    public final void setStatsLogger(IStatisticLogger logger) {
        this.statsLogger = logger;
    }

    public void onEvent(Event event) {  }

    public abstract AbstractPlayer copy();

    // override this to provide information on the last decision taken
    public Map<AbstractAction, Map<String, Object>> getDecisionStats() {
        return Collections.emptyMap();
    }
    /**
     * Sets the epsilon to be used for exploration in all games in the tournament
     * This is when we want to add noise at the environmental level (e.g. for exploration during learning)
     * independently of any exploration at the individual agent level
     *
     * @param epsilon
     */
    public void setExploration(double epsilon) {
        exploreEpsilon = epsilon;
    }
}
