package uno;

import core.turnorder.AlternatingTurnOrder;

public class UnoTurnOrder extends AlternatingTurnOrder {
    public UnoTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    public void skip()
    {
        turnOwner = (nPlayers + turnOwner + direction) % nPlayers;
    }
}

