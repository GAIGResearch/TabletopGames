package core;

import core.actions.AbstractAction;
import utilities.Utils;

import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public abstract class AbstractForwardModel {

    // Random generator for this game.
    protected Random rnd;

    /* Limited access/Final methods */

    /**
     * Creates a new FM object with a given random seed.
     * @param seed - random seed or this forward model.
     */
    protected AbstractForwardModel(long seed) {
        rnd = new Random(seed);
    }

    /**
     * Empty constructor for copies.
     */
    protected AbstractForwardModel() {}

    /**
     * Combines both super class and sub class setup methods. Called from the game loop.
     * @param firstState - initial state.
     */
    final void _setup(AbstractGameState firstState) {
        abstractSetup(firstState);
        setup(firstState);
        firstState.allComponents.clear();
        firstState.addAllComponents();
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

        firstState.gamePhase = AbstractGameState.DefaultGamePhase.Main;
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
    protected abstract void setup(AbstractGameState firstState);

    /**
     * Performs any end of game computations, as needed. Not necessary to be implemented in the subclass, but can be.
     * The last thing to be called in the game loop, after the game is finished.
     */
    protected void endGame(AbstractGameState gameState) {}

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of IAction objects.
     */
    protected abstract List<AbstractAction> _computeAvailableActions(AbstractGameState gameState);

    /**
     * Gets a copy of the FM with a new random number generator.
     * @return - new forward model with different random seed (keeping logic).
     */
    protected abstract AbstractForwardModel getCopy();

    /* Public API */

    /**
     * Applies the given action to the game state and executes any other game rules.
     * @param currentState - current game state, to be modified by the action.
     * @param action - action requested to be played by a player.
     */
    public abstract void next(AbstractGameState currentState, AbstractAction action);

    /**
     * Sets up the given game state for game start according to game rules, with a new random seed.
     * @param gameState - game state to be modified.
     */
    public final void setupGameState(AbstractGameState gameState) {
        abstractSetup(gameState);
        setup(gameState);
    }

    /**
     * Computes the available actions and updates the game state accordingly.
     * @param gameState - game state to update with the available actions.
     * @return - the list of actions available.
     */
    public final List<AbstractAction> computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = _computeAvailableActions(gameState);
        gameState.setAvailableActions(actions);
        return Collections.unmodifiableList(actions);
    }

    /**
     * Returns a copy of this forward model with a new random seed.
     * @return a new Forward Model instance with a different random object.
     */
    public AbstractForwardModel copy() {
        AbstractForwardModel model = getCopy();
        model.rnd = new Random();  // TODO: there are 2 random sources: given by gameSeed in GameParameters, and here
        return model;
    }
}
