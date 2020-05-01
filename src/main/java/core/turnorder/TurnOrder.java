package core.turnorder;

import core.AbstractGameState;
import players.AbstractPlayer;

import java.util.List;

public abstract class TurnOrder {

    protected int nPlayers;
    int currentPlayer = 0;
    protected int turnCounter;
    public final int getTurnCounter() {return turnCounter;}
    public final int getCurrentPlayer(){
        return currentPlayer;
    }

    public TurnOrder(int nPlayers) {
        this.nPlayers = nPlayers;
    }

    public abstract void endPlayerTurn(AbstractGameState gameState);
}
