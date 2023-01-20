package core;

import core.actions.AbstractAction;
import evaluation.metrics.Event;

import static core.CoreConstants.GameResult.GAME_ONGOING;
import static evaluation.metrics.Event.GameEvent.ROUND_OVER;
import static evaluation.metrics.Event.GameEvent.TURN_OVER;

public abstract class StandardForwardModel extends AbstractForwardModel {

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        _beforeAction(currentState, action);
        if (action != null) {
            action.execute(currentState);
        } else {
            throw new AssertionError("No action selected by current player");
        }
        _afterAction(currentState, action);
    }

    /**
     * This is a method hook for any game-specific functionality that should run before an Action is executed
     * by the forward model
     *
     * @param currentState
     * @param actionTaken
     */
    protected void _beforeAction(AbstractGameState currentState, AbstractAction actionTaken) {
        // override if needed
    }

    /**
     * This is a method hook for any game-specific functionality that should run after an Action is executed
     * by the forward model
     *
     * @param currentState
     * @param actionTaken
     */
    protected void _afterAction(AbstractGameState currentState, AbstractAction actionTaken){
        // override if needed
    }

    /**
     * The default assumption is that after a player has in finished their turn, play will proceed
     * sequentially to the next player, looping back to player zero once all players have acted.
     * If this is not the case, then use the alternative method with a specific argument for the next player
     *
     * It is the responsibility of the game-specific forward model that extends this class to call endPlayerTurn()
     *
     * @param gs
     */
    @Override
    public final void endPlayerTurn(AbstractGameState gs) {
        endPlayerTurn(gs, (gs.nPlayers + gs.turnOwner + 1) % gs.nPlayers);
    }

    /**
     * End the player turn. This will publish a TURN_OVER event, and change the currentPlayer on the
     * game state to be the one specified.
     *
     * @param gs
     */
    public final void endPlayerTurn(AbstractGameState gs, int nextPlayer) {
        if (gs.getGameStatus() != GAME_ONGOING) return;

        gs.getPlayerTimer()[gs.getCurrentPlayer()].incrementTurn();
        gs.listeners.forEach(l -> l.onEvent(Event.createEvent(TURN_OVER, gs)));
        gs.turnCounter++;
        gs.turnOwner = nextPlayer;
    }

    /**
     * Method executed at the end of a Round (however that is defined in a game)
     * It publishes a ROUND_OVER event.
     * It resets the turn counter, sets the currentPlayer to the one specified, and increments the Round counter.
     * If maximum number of rounds reached, game ends.
     * If there are no players still playing, game ends and method returns.
     *
     *  It is the responsibility of the game-specific forward model that extends this class to call endPlayerTurn()
     *
     * @param gs - current game state.
     */
    public final void endRound(AbstractGameState gs, int firstPlayerOfNextRound) {
        if (gs.getGameStatus() != GAME_ONGOING) return;

        gs.getPlayerTimer()[gs.getCurrentPlayer()].incrementRound();

        gs.listeners.forEach(l -> l.onEvent(Event.createEvent(ROUND_OVER, gs)));
        if (gs.getCoreGameParameters().recordEventHistory) {
            gs.recordHistory(ROUND_OVER.name());
        }

        gs.roundCounter++;
        if (gs.getGameParameters().maxRounds != -1 && gs.roundCounter == gs.getGameParameters().maxRounds) {
            endGame(gs);
        } else {
            gs.turnCounter = 0;
            gs.turnOwner = firstPlayerOfNextRound;
            gs.firstPlayer = firstPlayerOfNextRound;
        }
    }

    /**
     * A Forward Model should be stateless - and hence have no need to implement a _copy() method
     * @return
     */
    @Override
    public final StandardForwardModel _copy() {
        return this;
    }
}
