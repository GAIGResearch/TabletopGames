package core.turnorders;

import core.AbstractGameState;

public class StandardTurnOrder extends TurnOrder{

    public StandardTurnOrder(int nPlayers, int nMaxRounds) {
        super(nPlayers, nMaxRounds);
    }
    public StandardTurnOrder(int playerCount) {
        super(playerCount);
    }
    public StandardTurnOrder() {}

    @Override
    protected void _reset() {
        // nothing
    }

    @Override
    protected TurnOrder _copy() {
        return new StandardTurnOrder();
    }

    @Override
    public void _endRound(AbstractGameState gameState) {

    }

    @Override
    public void _startRound(AbstractGameState gameState) {

    }
}
