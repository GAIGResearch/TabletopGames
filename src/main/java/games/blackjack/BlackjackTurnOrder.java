package games.blackjack;

import core.turnorders.AlternatingTurnOrder;
import core.AbstractGameState;
import core.turnorders.TurnOrder;
import games.uno.UnoTurnOrder;

import java.util.Objects;
import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;

import static utilities.Utils.GameResult.GAME_ONGOING;

public class BlackjackTurnOrder extends AlternatingTurnOrder {
    private boolean skipTurn;


    public BlackjackTurnOrder(int nPlayers) {
        super(nPlayers);
        skipTurn = false;
        nMaxRounds = 1;
    }

    @Override
    protected void _reset(){
        super._reset();
        skipTurn = false;
    }

    public void skip(){
        skipTurn = true;
    }

    @Override
    public int nextPlayer(AbstractGameState gameState){
        int nextOwner = (nPlayers + turnOwner + direction) % nPlayers;
        if (skipTurn){
            skipTurn = false;
            return (nPlayers + nextOwner + direction) % nPlayers;
        }
        else
            return nextOwner;
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState){
        if (gameState.getGameStatus() != GAME_ONGOING) return;

        turnCounter++;
        if (turnCounter >= nPlayers) endRound(gameState);
        else {
            moveToNextPlayer(gameState, nextPlayer(gameState));
        }
    }

    @Override
    public void endRound(AbstractGameState gameState) {
        roundCounter++;
        if (nMaxRounds != -1 && roundCounter == nMaxRounds) {
            gameState.setGameStatus(GAME_END);
        }
        else {
            turnCounter = 0;
            moveToNextPlayer(gameState, firstPlayer);
        }
    }

    @Override
    protected TurnOrder _copy(){
        BlackjackTurnOrder bjto = new BlackjackTurnOrder(nPlayers);
        bjto.skipTurn = skipTurn;
        bjto.direction = direction;
        return bjto;
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof BlackjackTurnOrder)) return false;
        if (!super.equals(o)) return false;
        BlackjackTurnOrder that = (BlackjackTurnOrder) o;
        return skipTurn == that.skipTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), skipTurn);
    }
}
