package games.virus;

import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;

public class VirusTurnOrder extends AlternatingTurnOrder {
    public VirusTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    protected TurnOrder _copy() {
        return new VirusTurnOrder(nPlayers);
    }

}
