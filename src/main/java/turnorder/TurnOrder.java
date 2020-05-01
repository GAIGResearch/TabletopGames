package turnorder;

import core.AbstractGameState;
import players.AbstractPlayer;

import java.util.List;

public abstract class TurnOrder {

    protected List<AbstractPlayer> players;

    protected int turnCounter;
    public int getTurnCounter(){return turnCounter;}

    public abstract void endPlayerTurn(AbstractGameState gameState);

    public abstract AbstractPlayer getCurrentPlayer(AbstractGameState gameState);
}
