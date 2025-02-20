package core;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.interfaces.IPlayerDecorator;
import utilities.ActionTreeNode;
import utilities.ElapsedCpuChessTimer;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.GameResult.*;

public abstract class AbstractForwardModel {

    public ActionTreeNode root;
    public List<ActionTreeNode> leaves;

    // Decorator modify (restrict) the actions available to the player.
    // This enables the Forward Model to be passed to the decision algorithm (e.g. MCTS), and ensure that any
    // restrictions are applied to the actions available to the player during search, and not just
    // in the main game loop.
    protected List<IPlayerDecorator> decorators;
    protected int decisionPlayerID;

    /* Limited access/Final methods */

    public AbstractForwardModel() {
        this(new ArrayList<>(), -1);
    }

    public AbstractForwardModel(List<IPlayerDecorator> decorators, int playerID) {
        this.decorators = new ArrayList<>(decorators);
        this.decisionPlayerID = playerID;
    }

    /**
     * Combines both super class and sub class setup methods. Called from the game loop.
     *
     * @param firstState - initial state.
     */
    protected void abstractSetup(AbstractGameState firstState) {
        firstState.gameStatus = CoreConstants.GameResult.GAME_ONGOING;
        firstState.playerResults = new CoreConstants.GameResult[firstState.getNPlayers()];
        Arrays.fill(firstState.playerResults, CoreConstants.GameResult.GAME_ONGOING);
        firstState.gamePhase = CoreConstants.DefaultGamePhase.Main;
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

    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
        return _computeAvailableActions(gameState);
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
        disqualifyOrRandomAction(gameState.coreGameParameters.disqualifyPlayerOnIllegalActionPlayed, gameState);
    }

    /**
     * Either disqualify (automatic loss and no more playing), or play a random action for the player instead.
     * @param flag - boolean to check if player should be disqualified, or random action should be played
     * @param gameState - current game state
     */
    protected final AbstractAction disqualifyOrRandomAction(boolean flag, AbstractGameState gameState) {
        if (flag) {
            gameState.setPlayerResult(CoreConstants.GameResult.DISQUALIFY, gameState.getCurrentPlayer());
            endPlayerTurn(gameState);
            return new DoNothing();
        } else {
            List<AbstractAction> possibleActions = computeAvailableActions(gameState);
            int randomAction = new Random(gameState.getGameParameters().getRandomSeed()).nextInt(possibleActions.size());
            next(gameState, possibleActions.get(randomAction));
            return possibleActions.get(randomAction);
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
            int player = currentState.getCurrentPlayer();
            currentState.recordAction(action, player);
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
        // If there is an action in progress (see IExtendedSequence), then delegate to that
        List<AbstractAction> retValue;
        if (gameState.isActionInProgress()) {
            retValue = gameState.actionsInProgress.peek()._computeAvailableActions(gameState, actionSpace);
        } else if (actionSpace != null && !actionSpace.isDefault()) {
            retValue = _computeAvailableActions(gameState, actionSpace);
        } else {
            retValue = _computeAvailableActions(gameState);
        }

        // Then apply Decorators regardless of source of actions
        for (IPlayerDecorator decorator : decorators) {
            if (decorator.decisionPlayerOnly() && gameState.getCurrentPlayer() != decisionPlayerID)
                continue;
            retValue = decorator.actionFilter(gameState, retValue);
        }
        return retValue;
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

    public void addPlayerDecorator(IPlayerDecorator decorator) {
        decorators.add(decorator);
    }
    public void clearPlayerDecorators() {
        decorators.clear();
    }
}
