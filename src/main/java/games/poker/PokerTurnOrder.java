package games.poker;

import core.AbstractGameState;
import core.CoreConstants;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;

import static utilities.Utils.GameResult.*;

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
        int nTries = 1;
        while ((pgs.playerFold[next] || pgs.getPlayerResults()[next] == LOSE) && nTries <= gameState.getNPlayers()) {
            next = (nPlayers + next + direction) % nPlayers;
            nTries++;
        }
        if (nTries > gameState.getNPlayers()) {
            gameState.setGameStatus(GAME_END);
        }
        return next;
    }

    public void fold(PokerGameState pgs, int player) {
        if (player == firstPlayer) {
            // Move first player to next one
            firstPlayer = (nPlayers + firstPlayer + direction) % nPlayers;
            int nTries = 1;
            while ((pgs.playerFold[firstPlayer] || pgs.getPlayerResults()[firstPlayer] == LOSE) && nTries <= pgs.getNPlayers()) {
                firstPlayer = (nPlayers + firstPlayer + direction) % nPlayers;
                nTries++;
            }
            if (nTries > pgs.getNPlayers()) {
                pgs.setGameStatus(GAME_END);
            }
        }
        endPlayerTurn(pgs);
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

    @Override
    protected PokerTurnOrder _copy() {
        PokerTurnOrder to = new PokerTurnOrder(nPlayers);
        to.direction = direction;
        return to;
    }
}
