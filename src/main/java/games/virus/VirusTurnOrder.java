package games.virus;

import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;

public class VirusTurnOrder extends AlternatingTurnOrder {
    public VirusTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    public TurnOrder copy() {
        VirusTurnOrder to = new VirusTurnOrder(nPlayers);
        return copyTo(to);
    }

}
