package games.DiceMonastery;

import core.AbstractGameState;
import core.turnorders.TurnOrder;

import static games.DiceMonastery.DiceMonasteryTurnOrder.Season.*;

public class DiceMonasteryTurnOrder extends TurnOrder {

    public DiceMonasteryTurnOrder(int nPlayers) {
        super(nPlayers);
    }

    enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    @Override
    public void endPlayerTurn(AbstractGameState gameState) {
        super.endPlayerTurn(gameState);
    }

    @Override
    public void endRound(AbstractGameState gameState) {
        super.endRound(gameState);
    }

    @Override
    public int getCurrentPlayer(AbstractGameState gameState) {
        return super.getCurrentPlayer(gameState);
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        return super.nextPlayer(gameState);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    Season season = SPRING;
    int year = 1;

    @Override
    protected void _reset() {
        season = SPRING;
        year = 1;
    }

    @Override
    protected DiceMonasteryTurnOrder _copy() {
        DiceMonasteryTurnOrder retValue = new DiceMonasteryTurnOrder(nPlayers);
        retValue.season = season;
        retValue.year = year;
        return retValue;
    }

}
