package games.pandemic;

import core.AbstractGameState;
import core.turnorder.ReactiveTurnOrder;

public class PandemicTurnOrder extends ReactiveTurnOrder {
    PandemicTurnOrder(int nPlayers, int nActionsPerTurn){
        super(nPlayers, nActionsPerTurn, -1);
    }

    @Override
    public void endPlayerTurnStep(AbstractGameState gameState) {
        if (reactivePlayers.size() > 0) reactivePlayers.poll();
        else turnStep++;
    }
}
