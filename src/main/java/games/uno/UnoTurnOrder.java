package games.uno;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;

import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class UnoTurnOrder extends AlternatingTurnOrder {

    private boolean skipTurn;

    public UnoTurnOrder(int nPlayers) {
        super(nPlayers);
        skipTurn = false;
    }

    @Override
    protected void _reset() {
        super._reset();
        skipTurn = false;
    }

    public void skip()
    {
        skipTurn = true;
        //turnOwner = (nPlayers + turnOwner + direction) % nPlayers;
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        int nextOwner = (nPlayers + turnOwner + direction) % nPlayers;
        if (skipTurn) {
            skipTurn = false;
            return (nPlayers + nextOwner + direction) % nPlayers;
        }
        else
            return nextOwner;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        turnCounter++;
        moveToNextPlayer(gameState, nextPlayer(gameState));
    }

    @Override
    protected TurnOrder _copy() {
        UnoTurnOrder uto = new UnoTurnOrder(nPlayers);
        uto.skipTurn = skipTurn;
        uto.direction = direction;
        return uto;
    }
}

