package games.virus;

import core.turnorder.TurnOrder;

public class VirusTurnOrder extends TurnOrder {
    public VirusTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    public TurnOrder copy() {
        VirusTurnOrder to = new VirusTurnOrder(nPlayers);
        return copyTo(to);
    }

}
