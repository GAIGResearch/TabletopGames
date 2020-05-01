package games.pandemic;

import core.AbstractGameState;
import players.AbstractPlayer;
import core.turnorder.TurnOrder;

import java.util.List;

public class PandemicTurnOrder extends TurnOrder {
    PandemicTurnOrder(int nPlayers){
        super(nPlayers);
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {

    }
}
