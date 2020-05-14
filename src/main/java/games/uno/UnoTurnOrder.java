package games.uno;

import core.AbstractGameState;
import core.turnorder.AlternatingTurnOrder;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class UnoTurnOrder extends AlternatingTurnOrder {

    private boolean skipTurn;

    public UnoTurnOrder(int nPlayers) {
        super(nPlayers);
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
        turnCounter++;
        turnOwner = nextPlayer(gameState);
        while (gameState.getPlayerResults()[turnOwner] != GAME_ONGOING) {
            turnOwner = nextPlayer(gameState);
        }
    }
}

