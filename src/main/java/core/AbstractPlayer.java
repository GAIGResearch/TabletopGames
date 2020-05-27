package core;

import core.actions.AbstractAction;
import core.observations.IObservation;

import java.util.List;

public abstract class AbstractPlayer {

    int playerID;
    ForwardModel forwardModel;

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
    public final ForwardModel getForwardModel() {
        return forwardModel;
    }

    /**
     * Initialize agent given an observation of the initial game-state.
     * @param observation observation of the initial game-state
     */
    public abstract int getAction(IObservation observation, List<AbstractAction> actions);

    /**
     * Initialize agent given an observation of the initial game-state.
     * e.g. load weights, initialize neural network
     * @param observation observation of the initial game-state
     */
    public void initializePlayer(IObservation observation) {}

    /**
     * Receive an updated game-state for which it is not required to respond with an action.
     */
    public void registerUpdatedObservation(IObservation observation) {}

    /**
     * Finalize agent given an observation of the final game-state.
     * e.g. store variables after training, modify weights, etc.
     * @param observation observation of the final game-state
     */
    public void finalizePlayer(IObservation observation) {}
}
