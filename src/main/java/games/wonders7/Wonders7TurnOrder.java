package games.wonders7;

import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;

class Wonders7TurnOrder extends AlternatingTurnOrder {
    // track whose turn it is, and move on to the next player correctly
    // If the game has a simple alternating structure of each player taking their turn in order,
    // then you can just use AlternatingTurnOrder

    Wonders7TurnOrder(int nPlayers){
        super(nPlayers);
    }

    @Override
    protected void _reset() {

    }
    public int getDirection(){
        return direction;
    }

    @Override
    protected TurnOrder _copy() {
        return new Wonders7TurnOrder(nPlayers);
    }

}