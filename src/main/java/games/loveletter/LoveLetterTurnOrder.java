package games.loveletter;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;

import static utilities.Utils.GameResult.GAME_ONGOING;


public class LoveLetterTurnOrder extends AlternatingTurnOrder {

    public LoveLetterTurnOrder(int nPlayers){
        super(nPlayers);
        setStartingPlayer(0);
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) {
            return;
        }

        gameState.getPlayerTimer()[getCurrentPlayer(gameState)].incrementTurn();

        turnCounter++;
        moveToNextPlayer(gameState, nextPlayer(gameState));
    }

    @Override
    protected TurnOrder _copy() {
        LoveLetterTurnOrder to = new LoveLetterTurnOrder(nPlayers);
        to.direction = direction;
        return to;
    }
}
