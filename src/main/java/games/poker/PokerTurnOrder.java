package games.poker;

import core.AbstractGameState;
import core.turnorders.AlternatingTurnOrder;
import core.turnorders.TurnOrder;
import games.uno.UnoTurnOrder;

import java.util.Objects;

import static utilities.Utils.GameResult.GAME_END;
import static utilities.Utils.GameResult.GAME_ONGOING;

public class PokerTurnOrder extends AlternatingTurnOrder {

    private boolean skipTurn;

    public PokerTurnOrder(int nPlayers) {
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
        PokerTurnOrder pto = new PokerTurnOrder(nPlayers);
        pto.skipTurn = skipTurn;
        pto.direction = direction;
        return pto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnoTurnOrder)) return false;
        if (!super.equals(o)) return false;
        PokerTurnOrder that = (PokerTurnOrder) o;
        return skipTurn == that.skipTurn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), skipTurn);
    }
}
