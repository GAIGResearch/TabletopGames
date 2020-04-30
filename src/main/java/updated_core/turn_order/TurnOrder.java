package updated_core.turn_order;

import updated_core.gamestates.AbstractGameState;
import updated_core.players.AbstractPlayer;

public abstract class TurnOrder {

    public abstract void endPlayerTurn(AbstractGameState gameState);

    public abstract AbstractPlayer getCurrentPlayer(AbstractGameState gameState);
}
