package core;

import core.actions.AbstractAction;
import utilities.Utils;

import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;

import static core.CoreConstants.DISQUALIFY_PLAYER_ON_ILLEGAL_ACTION_PLAYED;

public abstract class AbstractForwardModel {

    /* Limited access/Final methods */

    /**
     * Combines both super class and sub class setup methods. Called from the game loop.
     * @param firstState - initial state.
     */
    final void abstractSetup(AbstractGameState firstState) {
        firstState.availableActions = new ArrayList<>();
        firstState.gameStatus = Utils.GameResult.GAME_ONGOING;
        firstState.playerResults = new Utils.GameResult[firstState.getNPlayers()];
        Arrays.fill(firstState.playerResults, Utils.GameResult.GAME_ONGOING);
        firstState.gamePhase = AbstractGameState.DefaultGamePhase.Main;

        _setup(firstState);
        firstState.addAllComponents();
    }

    /* Methods to be implemented by subclasses, unavailable to AI players */

    /**
     * Performs initial game setup according to game rules
     *  - sets up decks and shuffles
     *  - gives player cards
     *  - places tokens on boards
     *  etc.
     * @param firstState - the state to be modified to the initial game state.
     */
    protected abstract void _setup(AbstractGameState firstState);

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param currentState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    protected abstract void _next(AbstractGameState currentState, AbstractAction action);

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of IAction objects.
     */
    protected abstract List<AbstractAction> _computeAvailableActions(AbstractGameState gameState);

    /**
     * Gets a copy of the FM with a new random number generator.
     * @return - new forward model with different random seed (keeping logic).
     */
    protected abstract AbstractForwardModel _copy();

    /**
     * Performs any end of game computations, as needed. Not necessary to be implemented in the subclass, but can be.
     * The last thing to be called in the game loop, after the game is finished.
     */
    protected void endGame(AbstractGameState gameState) {}

    /**
     * Current player tried to play an illegal action. Either disqualify (Automatic loss and no more playing),
     * or play a random action for them instead.
     * Subclasses can overwrite for their own behaviour.
     * @param gameState - game state in which illegal action was attempted.
     */
    protected void illegalActionPlayed(AbstractGameState gameState) {
        if (DISQUALIFY_PLAYER_ON_ILLEGAL_ACTION_PLAYED) {
            gameState.setPlayerResult(Utils.GameResult.DISQUALIFY, gameState.getCurrentPlayer());
            gameState.turnOrder.endPlayerTurn(gameState);
        } else {
            int randomAction = new Random(gameState.getGameParameters().getGameSeed()).nextInt(gameState.getActions().size());
            next(gameState, gameState.getActions().get(randomAction));
        }
    }

    /* ###### Public API for AI players ###### */

    /**
     * Sets up the given game state for game start according to game rules, with a new random seed.
     * @param gameState - game state to be modified.
     */
    public final void setup(AbstractGameState gameState) {
        abstractSetup(gameState);
    }

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param currentState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    public final void next(AbstractGameState currentState, AbstractAction action) {
        if (action != null && currentState.getActions().contains(action)) {
            _next(currentState, action);
        } else {
            System.out.println("Invalid action.");
            illegalActionPlayed(currentState);
        }
    }

    /**
     * Computes the available actions and updates the game state accordingly.
     * @param gameState - game state to update with the available actions.
     * @return - the list of actions available.
     */
    public final List<AbstractAction> computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = _computeAvailableActions(gameState);
        gameState.setAvailableActions(actions);
        return actions;
    }

    /**
     * Returns a copy of this forward model with a new random seed.
     * @return a new Forward Model instance with a different random object.
     */
    public final AbstractForwardModel copy() {
        return _copy();
    }
}
