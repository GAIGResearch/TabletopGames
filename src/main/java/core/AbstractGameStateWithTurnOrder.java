package core;

import core.turnorders.TurnOrder;
import evaluation.listeners.IGameListener;

/**
 * Contains all game state information.
 * <p>
 * This is distinct from the Game, of which it is a component. The Game also controls the players in the game, and
 * this information is not present in (and must not be present in) the AbstractGameState.
 * <p>
 * A copy of the AbstractGameState is provided to each AbstractPlayer when it is their turn to act.
 * Separately the AbstractPlayer has a ForwardModel to be used if needed - this caters for the possibility that
 * agents may want to use a different/learned forward model in some use cases.
 */
@Deprecated
public abstract class AbstractGameStateWithTurnOrder extends AbstractGameState {

    protected TurnOrder turnOrder;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players in the game
     */
    public AbstractGameStateWithTurnOrder(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
        this.turnOrder = _createTurnOrder(nPlayers);
        reset();
    }

    protected abstract TurnOrder _createTurnOrder(int nPlayers);

    /**
     * Resets variables initialised for this game state.
     */
    @Override
    protected void reset() {
        super.reset();
        turnOrder.reset();
    }

    public final TurnOrder getTurnOrder() {
        return turnOrder;
    }
    @Override
    public int getRoundCounter() {return turnOrder.getRoundCounter();}
    @Override
    public int getTurnCounter() {return turnOrder.getTurnCounter();}
    @Override
    public int getFirstPlayer() {return turnOrder.getFirstPlayer();}
    @Override
    public int getNPlayers() {return turnOrder.nPlayers();}
    @Override
    public int getCurrentPlayer() {return turnOrder.getCurrentPlayer(this);}

    @Override
    public void setTurnOwner(int newTurnOwner) {turnOrder.setTurnOwner(newTurnOwner);}
    @Override
    public void setFirstPlayer(int newFirstPlayer) {turnOrder.setStartingPlayer(newFirstPlayer);}
    public final void setTurnOrder(TurnOrder turnOrder) {
        this.turnOrder = turnOrder;
    }


    public void addListener(IGameListener listener) {
        turnOrder.addListener(listener);
    }

    public void clearListeners() {
        turnOrder.clearListeners();
    }

    /**
     * Create a copy of the game state containing only those components the given player can observe (if partial
     * observable).
     *
     * @param playerId - player observing this game state.
     */
    protected  AbstractGameStateWithTurnOrder _copy(int playerId) {
        AbstractGameStateWithTurnOrder retValue = __copy(playerId);
        retValue.turnOrder = turnOrder.copy();
        return retValue;
    }

    protected abstract AbstractGameStateWithTurnOrder __copy(int playerId);

    /**
     * Override the hashCode as needed for individual game states
     * Equality of hashcodes can sometimes require excluding allComponents from the hash
     * (It is OK for two java objects to be not equal and have the same hashcode)
     * @return
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + turnOrder.hashCode();
        return result;
    }
}
