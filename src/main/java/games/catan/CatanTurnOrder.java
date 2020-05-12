package games.catan;

import core.turnorder.TurnOrder;

public class CatanTurnOrder extends TurnOrder {

    public CatanTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
    }

    @Override
    public TurnOrder copy() {
        return null;
    }
}
