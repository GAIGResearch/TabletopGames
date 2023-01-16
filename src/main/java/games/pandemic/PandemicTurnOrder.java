package games.pandemic;

import core.AbstractGameState;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;

import java.util.LinkedList;
import java.util.Objects;

import static core.CoreConstants.GameResult.GAME_ONGOING;

public class PandemicTurnOrder extends ReactiveTurnOrder {
    protected int nStepsPerTurn;  // Number of steps in a turn before player's turn is finished
    protected int turnStep;  // 1 turn = n steps (by default n = 1)

    PandemicTurnOrder(int nPlayers, int nActionsPerTurn) {
        super(nPlayers);
        turnStep = 0;
        this.nStepsPerTurn = nActionsPerTurn;
    }

    public void endPlayerTurnStep() {
        if (reactivePlayers.size() > 0) reactivePlayers.poll();
        else turnStep++;
    }

    public int getTurnStep() {
        return turnStep;
    }

    @Override
    protected void _reset() {
        super._reset();
        turnStep = 0;
    }

    /**
     * Method executed after a player's turn is finished.
     * By default it resets the turnStep counter to 0 and increases the turn counter.
     * Then moves to the next alive player. If this is the last player, the round ends.
     * If the game has ended, turn owner is not changed. If there are no players still playing, game ends and method returns.
     *
     * @param gameState - current game state.
     */
    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        gameState.getPlayerTimer()[getCurrentPlayer(gameState)].incrementTurn();

        turnCounter++;
        if (turnCounter >= nPlayers) endRound(gameState);
        else {
            turnStep = 0;
            moveToNextPlayer(gameState, nextPlayer(gameState));
        }
    }


    /**
     * Method executed after all player turns.
     * By default it resets the turn counter, the turn owner to the first alive player and increases round counter.
     * If maximum number of rounds reached, game ends.
     *
     * @param gameState - current game state.
     */
    public void _startRound(AbstractGameState gameState) {
        turnStep = 0;
    }

    @Override
    protected TurnOrder _copy() {
        PandemicTurnOrder pto = new PandemicTurnOrder(nPlayers, nStepsPerTurn);
        pto.reactivePlayers = new LinkedList<>(reactivePlayers);
        pto.turnStep = turnStep;
        pto.nStepsPerTurn = nStepsPerTurn;
        return pto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PandemicTurnOrder)) return false;
        if (!super.equals(o)) return false;
        PandemicTurnOrder that = (PandemicTurnOrder) o;
        return nStepsPerTurn == that.nStepsPerTurn &&
                turnStep == that.turnStep;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nStepsPerTurn, turnStep);
    }
}
