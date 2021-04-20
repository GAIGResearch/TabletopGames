package games.dominion;

import core.turnorders.TurnOrder;

public class DominionTurnOrder extends TurnOrder {
    public DominionTurnOrder(int nPlayers) {
        super(nPlayers);
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
        return new DominionTurnOrder(nPlayers);
    }


}
