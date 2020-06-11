package games.descent;

import core.turnorders.AlternatingTurnOrder;

public class DescentTurnOrder extends AlternatingTurnOrder {

    // TODO: order is player, overlord(monster group 1), player, overlord (monster group 2) ...
    public DescentTurnOrder(int nPlayers) {
        super(nPlayers);
    }
}
