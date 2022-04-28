package core.turnorders;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.LogEvent;
import core.interfaces.IGameListener;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;

public abstract class TurnOrder {

    // Fixed
    protected int nPlayers;  // Number of players in the game
    protected int nMaxRounds;  // Number of rounds until the game is finished; -1 if infinite

    // Variable
    protected int firstPlayer;  // ID of first player to get a turn in a round
    protected int turnOwner;  // Owner of current turn
    protected int turnCounter;  // Number of turns in this round
    protected int roundCounter;  // 1 round = (1 turn) x nPlayers(alive)

    protected List<IGameListener> listeners = new ArrayList<>();

    public TurnOrder(int nPlayers, int nMaxRounds) {
        reset();
        this.nPlayers = nPlayers;
        this.nMaxRounds = nMaxRounds;
    }

    public TurnOrder(int nPlayers) {
        reset();
        this.nPlayers = nPlayers;
    }

    public TurnOrder() {}

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


    /* Limited access or final methods */

    /**
     * Resets the state of this turn order object to its initial state.
     */
    protected abstract void _reset();

    /**
     * Returns a copy of this TurnOrder object.
     * - Create a new object of the class.
     * - Copy subclass parameters.
     * @return - copy of TurnOrder.
     */
    protected abstract TurnOrder _copy();

    /**
     * Copies the properties of the super turn order class into the given instance.
     * @param turnOrder - instance of a turn order.
     * @return - the instance of the turn order with the properties of the super class copied.
     */
    protected final TurnOrder copyTo (TurnOrder turnOrder) {
        turnOrder.turnOwner = turnOwner;
        turnOrder.turnCounter = turnCounter;
        turnOrder.roundCounter = roundCounter;
        turnOrder.firstPlayer = firstPlayer;
        turnOrder.nMaxRounds = nMaxRounds;
        turnOrder.nPlayers = nPlayers;
        // we deliberately do not copy the listeners, as they only apply to the master turnorder
        return turnOrder;
    }

    /**
     * Resets this turn order object (only the variables that can change, not the fixed ones).
     */
    public final void reset() {
        _reset();
        firstPlayer = 0;
        turnOwner = 0;
        turnCounter = 0;
        roundCounter = 0;
    }

    /**
     * Returns a copy of this TurnOrder object.
     * - Create a new object of a class.
     * - Copy subclass parameters.
     * - Call TurnOrder.copyTo(object) method to copy super class parameters and return result.
     * @return - copy of TurnOrder.
     */
    public final TurnOrder copy() {
        TurnOrder to = _copy();
        return copyTo(to);
    }

    /* Public API. Can be overwritten by subclasses */

    /**
     * Method executed after a player's turn is finished.
     * By default it resets the turnStep counter to 0 and increases the turn counter.
     * Then moves to the next alive player. If this is the last player, the round ends.
     * If the game has ended, turn owner is not changed. If there are no players still playing, game ends and method returns.
     * @param gameState - current game state.
     */
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        gameState.getPlayerTimer()[getCurrentPlayer(gameState)].incrementTurn();

        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.TURN_OVER, gameState, null));

        turnCounter++;
        if (turnCounter >= nPlayers) endRound(gameState);
        else {
            moveToNextPlayer(gameState, nextPlayer(gameState));
        }
    }

    // helper function to avoid time-consuming string manipulations is the message is not actually
    // going to be logged anywhere
    public void logEvent(Supplier<String> eventText, AbstractGameState state) {
        if (listeners.isEmpty())
            return; // to avoid expensive string manipulations
        logEvent(eventText.get(), state);
    }
    public void logEvent(String eventText, AbstractGameState state) {
        AbstractAction logAction = new LogEvent(eventText);
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.GAME_EVENT, state, logAction));
    }

    /**
     * Method executed after all player turns.
     * By default it resets the turn counter, the turn owner to the first alive player and increases round counter.
     * If maximum number of rounds reached, game ends.
     * If there are no players still playing, game ends and method returns.
     * @param gameState - current game state.
     */
    public void endRound(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        gameState.getPlayerTimer()[getCurrentPlayer(gameState)].incrementRound();

        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, gameState, null));

        roundCounter++;
        if (nMaxRounds != -1 && roundCounter == nMaxRounds) {
            gameState.setGameStatus(GAME_END);
        }
        else {
            turnCounter = 0;
            moveToNextPlayer(gameState, firstPlayer);
        }
    }

    /**
     * Returns the current player acting in a given game state.
     * @param gameState - current game state.
     * @return int, current player ID.
     */
    public int getCurrentPlayer(AbstractGameState gameState) {
        if (gameState.isActionInProgress()) {
            // this is when things might differ from the default
            return gameState.currentActionInProgress().getCurrentPlayer(gameState);
        }
        return turnOwner;
    }

    /**
     * Sets the current turn owner
     * @param owner - new owner of turn
     */
    public void setTurnOwner(int owner) {
        this.turnOwner = owner;
    }

    /**
     * Calculates the next player who should have a turn.
     * @param gameState - current game state.
     * @return - int, player ID in range [0, nPlayers)
     */
    public int nextPlayer(AbstractGameState gameState) {
        return (nPlayers+turnOwner+1) % nPlayers;
    }

    /**
     * Moves to the given turn order. If the given player is not playing anymore, the next player still in the game
     * will be the turn owner instead.
     * @param gameState - current game state
     * @param newTurnOwner - new turn owner.
     */
    public final void moveToNextPlayer(AbstractGameState gameState, int newTurnOwner) {
        turnOwner = newTurnOwner;
        int n = 0;
        while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
            turnOwner = nextPlayer(gameState);
            n++;
            if (n >= nPlayers) {
                gameState.setGameStatus(GAME_END);
                break;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TurnOrder turnOrder = (TurnOrder) o;
        return nPlayers == turnOrder.nPlayers &&
                turnOwner == turnOrder.turnOwner &&
                roundCounter == turnOrder.roundCounter &&
                firstPlayer == turnOrder.firstPlayer &&
                turnCounter == turnOrder.turnCounter &&
                nMaxRounds == turnOrder.nMaxRounds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nPlayers, turnOwner, turnCounter, roundCounter, firstPlayer, nMaxRounds);
    }

    public void addListener(IGameListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public int getFirstPlayer() {
        return firstPlayer;
    }
}
