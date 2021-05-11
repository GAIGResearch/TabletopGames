package games.sushigo;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import utilities.Utils;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class SGTurnOrder extends AlternatingTurnOrder {
    public SGTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if(gameState.getGameStatus() != Utils.GameResult.GAME_ONGOING) return;
        turnCounter++;
        moveToNextPlayer(gameState, nextPlayer(gameState));
    }

    @Override
    public void endRound(AbstractGameState gameState) {
        //super.endRound(gameState);
        if (gameState.getGameStatus() != GAME_ONGOING) return;
        SGGameState SGGS = (SGGameState) gameState;
        roundCounter++;

        //Draw new hands
        for (int i = 0; i < SGGS.getNPlayers(); i++){
            SGGS.getPlayerFields().get(i).clear();
            for (int j = 0; j < SGGS.cardAmount; j++)
            {
                SGGS.playerHands.get(i).add(SGGS.drawPile.draw());
            }
        }
    }
}
