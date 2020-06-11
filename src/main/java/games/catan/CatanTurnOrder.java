package games.catan;

import core.turnorders.AlternatingTurnOrder;
import core.turnorders.ReactiveTurnOrder;
import core.turnorders.TurnOrder;

public class CatanTurnOrder extends ReactiveTurnOrder {
    protected int turnStep;

    CatanTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
        turnStep = 0;
    }

    @Override
    protected void _reset() {
        super._reset();
        turnStep = 0;
    }

    @Override
    protected TurnOrder _copy() {
        CatanTurnOrder to = new CatanTurnOrder(nPlayers, nMaxRounds);
        to.turnStep = turnStep;
        return to;
    }
}
