package core;

import core.actions.AbstractAction;
import core.interfaces.IStatisticLogger;
import utilities.SummaryLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractPlayer {

    protected IStatisticLogger statsLogger = null;
    // ID of this player, assigned by the game
    int playerID;
    // Forward model for the game
    private AbstractForwardModel forwardModel;
    String name;

    /* Final methods */

    /**
     * Retrieves this player's ID, as set by the game.
     * @return - int, player ID
     */
    public final int getPlayerID() {
        return playerID;
    }

    /**
     * Retrieves the forward model for current game being played.
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
     *
     * Anything that has a reference to an AbstractPlayer can therefore pass a ForwardModel.
     * In competition mode this it is not currently possible for opponents to get a link to your
     * object...and this should be avoided.
     * @param model
     */
    public void setForwardModel(AbstractForwardModel model) {
        this.forwardModel = model;
    }

    /* Methods that should be implemented in subclass */

    /**
     * Generate a valid action to play in the game. Valid actions can be found by accessing
     * AbstractGameState.getActions()
     * @param gameState observation of the current game state
     */
    public abstract AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions);

    /* Methods that can be implemented in subclass */

    /**
     * Initialize agent given an observation of the initial game state.
     * e.g. load weights, initialize neural network
     * @param gameState observation of the initial game state
     */
    public void initializePlayer(AbstractGameState gameState) {}

    /**
     * Finalize agent given an observation of the final game state.
     * e.g. store variables after training, modify weights, etc.
     * @param gameState observation of the final game state
     */
    public void finalizePlayer(AbstractGameState gameState) {}

    /**
     * Receive an updated game state for which it is not required to respond with an action.
     * @param gameState observation of the current game state
     */
    public void registerUpdatedObservation(AbstractGameState gameState) {}

    public final void setName(String name) {this.name = name;}

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

    public abstract AbstractPlayer copy();

    // override this to provide information on the last decision taken
    public Map<AbstractAction, Map<String, Object>> getDecisionStats() {
            return Collections.emptyMap();
    }
}
