package core;

import core.actions.AbstractAction;
import core.components.Area;
import core.components.Component;
import core.components.PartialObservableDeck;
import core.interfaces.IComponentContainer;
import core.interfaces.IExtendedSequence;
import core.interfaces.IGamePhase;
import core.turnorders.TurnOrder;
import games.GameType;
import utilities.ElapsedCpuChessTimer;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.GameResult.*;
import static java.util.stream.Collectors.toList;


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
public abstract class AbstractGameStateWithTurnOrder extends AbstractGameState {

    protected TurnOrder turnOrder;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param turnOrder      - turn order for this game.
     */
    public AbstractGameStateWithTurnOrder(AbstractParameters gameParameters, TurnOrder turnOrder, GameType gameType) {
        super(gameParameters, gameType);
        this.turnOrder = turnOrder;
    }

    /**
     * Resets variables initialised for this game state.
     */
    void reset() {
        super.reset();
        turnOrder.reset();
    }

    public final TurnOrder getTurnOrder() {
        return turnOrder;
    }

    public final void setTurnOrder(TurnOrder turnOrder) {
        this.turnOrder = turnOrder;
    }

    /* Methods to be implemented by subclass, protected access. */

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
     * TODO: Remove this annoyance, make hashcode final, and add a _hashcode() abstract method
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
