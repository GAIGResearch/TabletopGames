package games.poker;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import evaluation.metrics.Event;

import java.util.Arrays;

import static core.CoreConstants.GameResult.*;

public class PokerTurnOrder extends AlternatingTurnOrder {
    int roundFirstPlayer;

    public PokerTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    @Override
    protected void _reset() {
        super._reset();
        roundFirstPlayer = 0;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        if (gameState.getGameStatus() != GAME_ONGOING) return;
        listeners.forEach(l -> l.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, gameState)));

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
            gameState.endGame();
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
                pgs.endGame();
            }
        }
        endPlayerTurn(pgs);
    }

    public int getRoundFirstPlayer() {
        return roundFirstPlayer;
    }

    @Override
    public void _endRound(AbstractGameState gameState) {
        PokerGameState pgs = (PokerGameState) gameState;
        Arrays.fill(pgs.playerFold, false);

        turnCounter = 0;
        turnOwner = roundFirstPlayer;
        roundFirstPlayer = nextPlayer(gameState);
        firstPlayer = roundFirstPlayer;
    }

    @Override
    protected PokerTurnOrder _copy() {
        PokerTurnOrder to = new PokerTurnOrder(nPlayers);
        to.direction = direction;
        to.roundFirstPlayer = roundFirstPlayer;
        return to;
    }
}
