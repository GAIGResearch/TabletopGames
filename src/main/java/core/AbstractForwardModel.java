package core;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.SimultaneousAction;
import core.interfaces.IPlayerDecorator;
import utilities.ActionTreeNode;
import utilities.ElapsedCpuChessTimer;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.GameResult.*;

public abstract class AbstractForwardModel {

    /* Limited access/Final methods */

    public AbstractForwardModel() {
    }

    /**
     * Combines both super class and subclass setup methods. Called from the game loop.
     *
     * @param firstState - initial state.
     */
    protected void abstractSetup(AbstractGameState firstState) {
        firstState.gameStatus = CoreConstants.GameResult.GAME_ONGOING;
        firstState.playerResults = new CoreConstants.GameResult[firstState.getNPlayers()];
        Arrays.fill(firstState.playerResults, CoreConstants.GameResult.GAME_ONGOING);
        firstState.gamePhase = CoreConstants.DefaultGamePhase.Main;

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
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, int activePlayer) {
        return _computeAvailableActions(gameState);
    }

    protected abstract List<AbstractAction> _computeAvailableActions(AbstractGameState gameState);

    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
        return _computeAvailableActions(gameState);
    }

    /**
     * Override this method if (and only if) you are implementing multiple action spaces for a game AND we have
     * simultaneous moves. By default a non-default action space is passed on to _computeAvailableActions(gameState, actionSpace)
     * (as in IExtendedSequence).
     */
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace, int activePlayer) {
        if (actionSpace == null || actionSpace.isDefault())
            return _computeAvailableActions(gameState, activePlayer);
        return _computeAvailableActions(gameState, actionSpace);
    }

    /**
     * This is applied to every list of actions returned by computeAvailableActions(), whether the actions come from
     * this forward model or from an IExtendedSequence. The default does nothing; see DecoratedForwardModel.
     *
     * @param gameState    - the state the actions were computed for
     * @param actions      - the available actions
     * @param activePlayer - the player the actions are for
     * @return - the actions to make available
     */
    protected List<AbstractAction> filterAvailableActions(AbstractGameState gameState, List<AbstractAction> actions, int activePlayer) {
        return actions;
    }

    protected abstract void endPlayerTurn(AbstractGameState state);

    /**
     * Current player tried to play an illegal action.
     * Subclasses can overwrite for their own behaviour.
     *
     * @param gameState - game state in which illegal action was attempted.
     * @param action    - action played
     */
    protected void illegalActionPlayed(AbstractGameState gameState, AbstractAction action) {
        if (gameState.coreGameParameters.disqualifyPlayerOnIllegalActionPlayed) {
            disqualify(gameState, gameState.getCurrentPlayer());
        } else {
            AbstractAction rndAction = takeRandomAction(gameState, gameState.getCurrentPlayer());
            next(gameState, rndAction);
        }
    }

    /**
     * Picks a random action for the player. The action is returned, not applied: the caller applies it.
     *
     * @param gameState - current game state
     * @param player    - the player for whom a random action is chosen
     */
    protected final AbstractAction takeRandomAction(AbstractGameState gameState, int player) {
        List<AbstractAction> possibleActions = computeAvailableActions(gameState, gameState.coreGameParameters.actionSpace, player);
        return possibleActions.get(gameState.getRnd().nextInt(possibleActions.size()));
    }

    /**
     * Disqualify the player: they lose, whatever happens later in the game.
     * If this leaves players from only one team, the game ends and they win.
     * Otherwise the game continues; the disqualified player makes no more decisions, and Game picks random actions
     * for them whenever the game needs them to act (this keeps the game's own rules and turn order intact).
     *
     * @param gameState - current game state
     * @param player    - the player to disqualify
     */
    protected final void disqualify(AbstractGameState gameState, int player) {
        gameState.setPlayerResult(DISQUALIFY, player);
        if (gameState.isDecidedByDisqualification()) {
            gameState.setGameStatus(GAME_END);
            for (int p = 0; p < gameState.getNPlayers(); p++)
                gameState.setPlayerResult(WIN_GAME, p);  // does not change the disqualified players
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
            if (action instanceof SimultaneousAction sa) {
                for (Map.Entry<Integer, AbstractAction> entry : sa.getPlayerActions().entrySet()) {
                    currentState.recordAction(entry.getValue(), entry.getKey());
                }
            } else {
                int player = currentState.getCurrentPlayer();
                currentState.recordAction(action, player);
            }

            _next(currentState, action);

        } else {
            if (currentState.coreGameParameters.verbose) {
                System.out.println("Invalid action.");
            }
            illegalActionPlayed(currentState, action);
        }
        currentState.advanceGameTick();
    }

    /**
     * Computes the available actions and updates the game state accordingly.
     *
     * @param gameState - game state to update with the available actions.
     * @return - the list of actions available.
     */
    public final List<AbstractAction> computeAvailableActions(AbstractGameState gameState) {
        return computeAvailableActions(gameState, gameState.coreGameParameters.actionSpace);
    }

    public final List<AbstractAction> computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
        return computeAvailableActions(gameState, actionSpace, gameState.getCurrentPlayer());
    }

    public final List<AbstractAction> computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace, int activePlayer) {
        // If there is an action in progress (see IExtendedSequence), then delegate to that
        List<AbstractAction> retValue;
        if (gameState.isActionInProgress()) {
            // we call appropriate method depending on the actionSpace and move simultaneity
            if (actionSpace != null && !actionSpace.isDefault()) {
                retValue = gameState.actionsInProgress.peek()._computeAvailableActions(gameState, actionSpace, activePlayer);
            } else {
                retValue = gameState.getActionsInProgress().peek()._computeAvailableActions(gameState, activePlayer);
            }
        } else if (actionSpace != null && !actionSpace.isDefault()) {
            retValue = _computeAvailableActions(gameState, actionSpace, activePlayer);
        } else {
            retValue = _computeAvailableActions(gameState, activePlayer);
        }
        return filterAvailableActions(gameState, retValue, activePlayer);
    }

    /**
     * Performs any end of game computations, as needed.
     * This should not normally need to be overriden - but can be. For example if a game is purely co-operative
     * or has an insta-win situation without the concept of a game score.
     * The last thing to be called in the game loop, after the game is finished.
     */
    protected void endGame(AbstractGameState gs) {
        gs.setGameStatus(CoreConstants.GameResult.GAME_END);
        // If we have more than one person in Ordinal position of 1, then this is a draw
        boolean drawn = IntStream.range(0, gs.getNPlayers()).map(gs::getOrdinalPosition).filter(i -> i == 1).count() > 1;
        for (int p = 0; p < gs.getNPlayers(); p++) {
            int o = gs.getOrdinalPosition(p);
            if (o == 1 && drawn)
                gs.setPlayerResult(DRAW_GAME, p);
            else if (o == 1)
                gs.setPlayerResult(WIN_GAME, p);
            else
                gs.setPlayerResult(LOSE_GAME, p);
        }
        if (gs.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(gs.getPlayerResults()));
        }
    }

    public void reset() {
        // default is that no reset is required, as a forward model does not have state
        // although it may have decoraters that do
    }

}
