package games.catan;

import core.turnorders.TurnOrder;

public class CatanTurnOrder extends TurnOrder {

    public CatanTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
    }

    @Override
    protected void _reset() {

    }

    @Override
    protected TurnOrder _copy() {
        return null;
    }
}
