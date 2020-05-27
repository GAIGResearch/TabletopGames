package core.turnorders;

import core.AbstractGameState;

public class AlternatingTurnOrder extends TurnOrder {
    int direction = 1;

    public AlternatingTurnOrder(int nPlayers){
        super(nPlayers);
    }

    @Override
    public TurnOrder copy() {
        AlternatingTurnOrder to = new AlternatingTurnOrder(nPlayers);
        to.direction = direction;
        return copyTo(to);
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        return (nPlayers + turnOwner + direction) % nPlayers;
    }

    public void reverse(){
        direction *= -1;
    }
}
