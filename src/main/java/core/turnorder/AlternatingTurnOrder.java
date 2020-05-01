package core.turnorder;

import core.AbstractGameState;

public class AlternatingTurnOrder extends TurnOrder {
    int direction = 1;

    public AlternatingTurnOrder(int nPlayers){
        super(nPlayers);
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        currentPlayer = (currentPlayer + direction) % nPlayers;
        turnCounter += 1;
    }

    public void reverse(){
        direction *= -1;
    }
}
