package games.poker;

import core.AbstractGameState;
import core.CoreConstants;
import core.turnorders.AlternatingTurnOrder;

import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class PokerTurnOrder extends AlternatingTurnOrder {

    public PokerTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;
        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.TURN_OVER, gameState, null));

        turnCounter++;
        moveToNextPlayer(gameState, nextPlayer(gameState));
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        int next = (nPlayers + turnOwner + direction) % nPlayers;
        PokerGameState pgs = (PokerGameState) gameState;
        while (pgs.playerFold[next]) {
            next = (nPlayers + next + direction) % nPlayers;
        }
        return next;
    }

    @Override
    public void endRound(AbstractGameState gameState) {

        if (gameState.getGameStatus() != GAME_ONGOING) return;

        gameState.getPlayerTimer()[getCurrentPlayer(gameState)].incrementRound();

        listeners.forEach(l -> l.onEvent(CoreConstants.GameEvents.ROUND_OVER, gameState, null));

        roundCounter++;
        if (nMaxRounds != -1 && roundCounter == nMaxRounds) {
            gameState.setGameStatus(GAME_END);
        }
        else {
            turnCounter = 0;
            turnOwner = firstPlayer;
            firstPlayer = nextPlayer(gameState);
            moveToNextPlayer(gameState, firstPlayer);
        }
    }
}
