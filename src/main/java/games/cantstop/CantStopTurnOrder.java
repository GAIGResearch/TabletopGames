package games.cantstop;

import core.turnorders.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

// TODO: Check if we need this, or can just use StandardOrder

public class CantStopTurnOrder extends StandardTurnOrder {

    public CantStopTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    protected void _reset() {

    }

    @Override
    protected TurnOrder _copy() {
        throw new NotImplementedException();
    }
}
