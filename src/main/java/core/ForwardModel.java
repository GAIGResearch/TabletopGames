package core;

import core.actions.AbstractAction;
import core.gamephase.DefaultGamePhase;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class ForwardModel {

    // Random generator for this game.
    protected Random rnd;

    /**
     * Creates a new FM object with a given random seed.
     * @param seed - random seed or this forward model.
     */
    protected ForwardModel(long seed) {
        rnd = new Random(seed);
    }

    /**
     * Combines both super class and sub class setup methods. Called from the game loop.
     * @param firstState - initial state.
     */
    final void _setup(AbstractGameState firstState) {
        abstractSetup(firstState);
        setup(firstState);
    }

    /**
     * Performs initialisation of variables in the abstract game state.
     * @param firstState - the initial game state.
     */
    private void abstractSetup(AbstractGameState firstState) {
        firstState.availableActions = new ArrayList<>();

        firstState.gameStatus = Utils.GameResult.GAME_ONGOING;
        firstState.playerResults = new Utils.GameResult[firstState.getNPlayers()];
        Arrays.fill(firstState.playerResults, Utils.GameResult.GAME_ONGOING);

        firstState.gamePhase = DefaultGamePhase.Main;
    }

    /**
     * Performs initial game setup according to game rules
     *  - sets up decks and shuffles
     *  - gives player cards
     *  - places tokens on boards
     *  etc.
     * @param firstState - the state to be modified to the initial game state.
     */
    protected abstract void setup(AbstractGameState firstState);

    /**
     * Performs any end of game computations, as needed. Not necessary to be implemented in the subclass, but can be.
     * The last thing to be called in the game loop, after the game is finished.
     */
    protected void endGame(AbstractGameState gameState) {}

    /* Public API */

    /**
     * Sets up the given game state for game start according to game rules, with a new random seed.
     * @param gameState - game state to be modified.
     */
    public final void setupGameState(AbstractGameState gameState) {
        abstractSetup(gameState);
        setup(gameState);
    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of IAction objects.
     */
    public abstract List<AbstractAction> computeAvailableActions(AbstractGameState gameState);

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param currentState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    public abstract void next(AbstractGameState currentState, AbstractAction action);
}
