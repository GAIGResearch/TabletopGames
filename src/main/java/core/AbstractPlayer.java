package core;

import core.actions.IAction;
import core.observations.IObservation;

import java.util.List;

public abstract class AbstractPlayer {

    int playerID;

    /**
     * Initialize agent given an observation of the initial game-state.
     * e.g. load weights, initialize neural network
     * @param observation observation of the initial game-state
     */
    public abstract void initializePlayer(IObservation observation);

    /**
     * Finalize agent given an observation of the final game-state.
     * e.g. store variables after training, modify weights, etc.
     * @param observation observation of the final game-state
     */
    public abstract void finalizePlayer(IObservation observation);

    /**
     * Initialize agent given an observation of the initial game-state.
     * @param observation observation of the initial game-state
     */
    public abstract int getAction(IObservation observation, List<IAction> actions);

    /**
     * Retrieves this player's ID, as set by the game.
     * @return - int, player ID
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * Receive an updated game-state for which it is not required to respond with an action.
     */
    public abstract void registerUpdatedObservation(IObservation observation);
}
