package core;

import core.actions.AbstractAction;
import utilities.ElapsedCpuChessTimer;
import utilities.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class AbstractForwardModel {

    /* Limited access/Final methods */

    /**
     * Combines both super class and sub class setup methods. Called from the game loop.
     *
     * @param firstState - initial state.
     */
    protected void abstractSetup(AbstractGameState firstState) {
        firstState.gameStatus = Utils.GameResult.GAME_ONGOING;
        firstState.playerResults = new Utils.GameResult[firstState.getNPlayers()];
        Arrays.fill(firstState.playerResults, Utils.GameResult.GAME_ONGOING);
        firstState.gamePhase = AbstractGameState.DefaultGamePhase.Main;
        firstState.playerTimer = new ElapsedCpuChessTimer[firstState.getNPlayers()];
        for (int i = 0; i < firstState.getNPlayers(); i++) {
            firstState.playerTimer[i] = new ElapsedCpuChessTimer(firstState.gameParameters.thinkingTimeMins,
                    firstState.gameParameters.incrementActionS, firstState.gameParameters.incrementTurnS,
                    firstState.gameParameters.incrementRoundS, firstState.gameParameters.incrementMilestoneS);
        }

        _setup(firstState);
        firstState.addAllComponents();
    }

    /* Methods to be implemented by subclasses, unavailable to AI players */

    /**
     * Performs initial game setup according to game rules
     * - sets up decks and shuffles
     * - gives player cards
     * - places tokens on boards
     * etc.
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    protected abstract void _setup(AbstractGameState firstState);

    /**
     * Applies the given action to the game state and executes any other game rules. Steps to follow:
     * - execute player action
     * - execute any game rules applicable
     * - check game over conditions, and if any trigger, set the gameStatus and playerResults variables
     * appropriately (and return)
     * - move to the next player where applicable
     *
     * @param currentState - current game state, to be modified by the action.
     * @param action       - action requested to be played by a player.
     */
    protected abstract void _next(AbstractGameState currentState, AbstractAction action);

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    protected abstract List<AbstractAction> _computeAvailableActions(AbstractGameState gameState);

    /**
     * Gets a copy of the FM with a new random number generator.
     *
     * @return - new forward model with different random seed (keeping logic).
     */
    protected abstract AbstractForwardModel _copy();

    /**
     * Performs any end of game computations, as needed. Not necessary to be implemented in the subclass, but can be.
     * The last thing to be called in the game loop, after the game is finished.
     */
    protected void endGame(AbstractGameState gameState) {
    }

    /**
     * Current player tried to play an illegal action.
     * Subclasses can overwrite for their own behaviour.
     *
     * @param gameState - game state in which illegal action was attempted.
     * @param action    - action played
     */
    protected void illegalActionPlayed(AbstractGameState gameState, AbstractAction action) {
        disqualifyOrRandomAction(gameState.coreGameParameters.disqualifyPlayerOnIllegalActionPlayed, gameState);
    }

    /**
     * Either disqualify (automatic loss and no more playing), or play a random action for the player instead.
     * @param flag - boolean to check if player should be disqualified, or random action should be played
     * @param gameState - current game state
     */
    protected final void disqualifyOrRandomAction(boolean flag, AbstractGameState gameState) {
        if (flag) {
            gameState.setPlayerResult(Utils.GameResult.DISQUALIFY, gameState.getCurrentPlayer());
            gameState.turnOrder.endPlayerTurn(gameState);
        } else {
            List<AbstractAction> possibleActions = computeAvailableActions(gameState);
            int randomAction = new Random(gameState.getGameParameters().getRandomSeed()).nextInt(possibleActions.size());
            next(gameState, possibleActions.get(randomAction));
        }
    }

    /* ###### Public API for AI players ###### */

    /**
     * Sets up the given game state for game start according to game rules, with a new random seed.
     *
     * @param gameState - game state to be modified.
     */
    public final void setup(AbstractGameState gameState) {
        gameState.reset();
        abstractSetup(gameState);
    }

    /**
     * Applies the given action to the game state and executes any other game rules.
     *
     * @param currentState - current game state, to be modified by the action.
     * @param action       - action requested to be played by a player.
     */
    public final void next(AbstractGameState currentState, AbstractAction action) {
        if (action != null) {
            if (currentState.isActionInProgress()) {
                // we register the action with the currently active ActionSequence
                currentState.currentActionInProgress().registerActionTaken(currentState, action);
            }
            _next(currentState, action);
            currentState.recordAction(action);
        } else {
            if (currentState.coreGameParameters.verbose) {
                System.out.println("Invalid action.");
            }
            illegalActionPlayed(currentState, action);
        }

        currentState.checkActionsInProgress();
    }

    /**
     * Computes the available actions and updates the game state accordingly.
     *
     * @param gameState - game state to update with the available actions.
     * @return - the list of actions available.
     */
    public final List<AbstractAction> computeAvailableActions(AbstractGameState gameState) {
        // If there is an action in progress (see IExtendedSequence), then delegate to that
        if (gameState.isActionInProgress()) {
            return gameState.actionsInProgress.peek()._computeAvailableActions(gameState);
        }
        return _computeAvailableActions(gameState);
    }

    /**
     * Returns a copy of this forward model with a new random seed.
     *
     * @return a new Forward Model instance with a different random object.
     */
    public final AbstractForwardModel copy() {
        return _copy();
    }
}
