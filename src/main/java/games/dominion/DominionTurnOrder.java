package games.dominion;

import core.AbstractGameState;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;

public class DominionTurnOrder extends TurnOrder {
    public DominionTurnOrder(int nPlayers) {
        super(nPlayers);
    }


    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        DominionGameState dgs = (DominionGameState) state;
        if (dgs.isActionInProgress()) {
            // this is when things might differ from the default
            return dgs.actionInProgress.getCurrentPlayer(dgs);
        }
        return super.getCurrentPlayer(state);
    }


    /**
     * Resets the state of this turn order object to its initial state.
     */
    @Override
    protected void _reset() {
        // no state to maintain
    }

    /**
     * Returns a copy of this TurnOrder object.
     * - Create a new object of the class.
     * - Copy subclass parameters.
     *
     * @return - copy of TurnOrder.
     */
    @Override
    protected TurnOrder _copy() {
        // no state
        return this;
    }


}
