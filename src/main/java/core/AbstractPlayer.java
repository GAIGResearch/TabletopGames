package core;

import core.actions.AbstractAction;
import core.interfaces.IStatisticLogger;
import utilities.SummaryLogger;

import java.util.List;

public abstract class AbstractPlayer {

    protected IStatisticLogger statsLogger = new SummaryLogger();
    // ID of this player, assigned by the game
    int playerID;
    // Forward model for the game
    AbstractForwardModel forwardModel;
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

    public void setName(String name) {this.name = name;}

    @Override
    public String toString() {
        return name;
    }

    public IStatisticLogger getStatsLogger() {
        return statsLogger;
    }

    public void setStatsLogger(IStatisticLogger logger) {
        this.statsLogger = logger;
    }
}
