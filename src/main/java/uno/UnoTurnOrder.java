package uno;

import core.AbstractGameState;
import players.AbstractPlayer;
import turnorder.TurnOrder;

import java.util.List;

public class UnoTurnOrder extends TurnOrder {
    UnoTurnOrder(List<AbstractPlayer> players){
        this.players = players;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
    }

    @Override
    public AbstractPlayer getCurrentPlayer(AbstractGameState gameState) {
        return null;
    }
}