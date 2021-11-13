package games.sushigo;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.SimultaneousTurnOrder;
import games.sushigo.cards.SGCard;
import utilities.Utils;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class SGTurnOrder extends SimultaneousTurnOrder {
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
    protected SGTurnOrder _copy() {
        return new SGTurnOrder(nPlayers);
    }

    @Override
    public void endRound(AbstractGameState gameState) {
        //super.endRound(gameState);
        if (gameState.getGameStatus() != GAME_ONGOING) return;
        SGGameState SGGS = (SGGameState) gameState;
        roundCounter++;

        for (int i = 0; i < SGGS.getNPlayers(); i++){
            //Clear fields ignoring dumplings
            for(int j = SGGS.getPlayerFields().get(i).getSize() - 1; j >= 0; j--)
            {
                if (SGGS.getPlayerFields().get(i).get(j).type != SGCard.SGCardType.Pudding)
                {
                    SGGS.getPlayerFields().get(i).remove(j);
                }
            }

            //Draw new hands
            for (int j = 0; j < SGGS.cardAmount; j++)
            {
                SGGS.playerHands.get(i).add(SGGS.drawPile.draw());
            }

            //Clear counters
            SGGS.setPlayerTempuraAmount(i, 0);
            SGGS.setPlayerSashimiAmount(i, 0);
            SGGS.setPlayerDumplingAmount(i, 0);
            SGGS.setPlayerWasabiAvailable(i, 0);
            SGGS.setPlayerChopSticksAmount(i, 0);
        }
    }
}
