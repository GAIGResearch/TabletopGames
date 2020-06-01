package core.turnorders;

import core.AbstractGameState;
import utilities.Utils;

import java.util.Objects;

import static utilities.Utils.GameResult.GAME_ONGOING;

public abstract class TurnOrder {

    // Fixed
    protected int nPlayers;  // Number of players in the game
    protected int firstPlayer;  // ID of first player to get a turn in a round
    protected int nMaxRounds;  // Number of rounds until the game is finished; -1 if infinite

    // Variable
    protected int turnOwner;  // Owner of current turn
    protected int turnCounter;  // Number of turns in this round
    protected int roundCounter;  // 1 round = (1 turn) x nPlayers(alive)

    public TurnOrder(int nPlayers, int nMaxRounds) {
        reset();
        this.nPlayers = nPlayers;
        this.nMaxRounds = nMaxRounds;
    }

    public TurnOrder(int nPlayers) {
        reset();
        this.nPlayers = nPlayers;
    }

    private void reset() {
        firstPlayer = 0;
        turnOwner = 0;
        turnCounter = 0;
        roundCounter = 0;
        nMaxRounds = -1;
    }


    public final void setStartingPlayer(int player) {
        firstPlayer = player;
        turnOwner = player;
    }
    public final int getTurnOwner(){
        return turnOwner;
    }
    public final int nPlayers() {
        return nPlayers;
    }
    public int getRoundCounter() {
        return roundCounter;
    }
    public int getTurnCounter() {
        return turnCounter;
    }


    /* The following can be overwritten by subclasses */

    /**
     * Method executed after a player's turn is finished.
     * By default it resets the turnStep counter to 0 and increases the turn counter.
     * Then moves to the next alive player. If this is the last player, the round ends.
     * @param gameState - current game state.
     */
    public void endPlayerTurn(AbstractGameState gameState) {
        turnCounter++;
        if (turnCounter >= nPlayers) endRound(gameState);
        else {
            turnOwner = nextPlayer(gameState);
            while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
            }
        }
    }

    /**
     * Method executed after all player turns.
     * By default it resets the turn counter, the turn owner to the first alive player and increases round counter.
     * If maximum number of rounds reached, game ends.
     * @param gameState - current game state.
     */
    public void endRound(AbstractGameState gameState) {
        roundCounter++;
        if (nMaxRounds != -1 && roundCounter == nMaxRounds) gameState.setGameStatus(Utils.GameResult.GAME_END);
        else {
            turnCounter = 0;
            turnOwner = 0;
            while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
                turnOwner = nextPlayer(gameState);
            }
        }
    }

    /**
     * Returns the current player acting in a given game state.
     * @param gameState - current game state.
     * @return int, current player ID.
     */
    public int getCurrentPlayer(AbstractGameState gameState) {
        return turnOwner;
    }

    /**
     * Calculates the next player who should have a turn.
     * @param gameState - current game state.
     * @return - int, player ID in range [0, nPlayers)
     */
    public int nextPlayer(AbstractGameState gameState) {
        return (turnOwner+1) % nPlayers;
    }

    /**
     * Returns a copy of this TurnOrder object.
     * - Create a new object of a class.
     * - Copy subclass parameters.
     * - Call TurnOrder.copyTo(object) method to copy super class parameters and return result.
     * @return - copy of TurnOrder.
     */
    public abstract TurnOrder copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TurnOrder turnOrder = (TurnOrder) o;
        return nPlayers == turnOrder.nPlayers &&
                turnOwner == turnOrder.turnOwner &&
                roundCounter == turnOrder.roundCounter &&
                firstPlayer == turnOrder.firstPlayer &&
                nMaxRounds == turnOrder.nMaxRounds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nPlayers, turnOwner, roundCounter, firstPlayer, nMaxRounds);
    }

    public TurnOrder copyTo (TurnOrder turnOrder) {
        turnOrder.turnOwner = turnOwner;
        turnOrder.roundCounter = roundCounter;
        turnOrder.firstPlayer = firstPlayer;
        turnOrder.nMaxRounds = nMaxRounds;
        return turnOrder;
    }
}
