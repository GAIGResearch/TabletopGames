package core;

import core.actions.AbstractAction;
import evaluation.metrics.Event;

import static core.CoreConstants.GameResult.GAME_ONGOING;

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

    protected abstract void _beforeAction(AbstractGameState currentState, AbstractAction actionTaken);

    protected abstract void _afterAction(AbstractGameState currentState, AbstractAction actionTaken);

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
        gs.listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, gs)));
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

        gs.listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.ROUND_OVER, gs)));
        if (gs.getCoreGameParameters().recordEventHistory) {
            gs.recordHistory(Event.GameEvent.ROUND_OVER.name());
        }

        gs.roundCounter++;
        if (gs.getGameParameters().maxRounds != -1 && gs.roundCounter == gs.getGameParameters().maxRounds) {
            gs.endGame();
        } else {
            gs.turnCounter = 0;
            gs.turnOwner = firstPlayerOfNextRound;
            gs.firstPlayer = firstPlayerOfNextRound;
        }
    }


    @Override
    public StandardForwardModel _copy() {
        return this;
    }
}
