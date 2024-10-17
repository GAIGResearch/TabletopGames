package core.turnorders;

import core.AbstractGameState;

import java.util.Objects;

public class AlternatingTurnOrder extends TurnOrder {
    protected int direction;

    protected AlternatingTurnOrder() {}

    public AlternatingTurnOrder(int nPlayers){
        super(nPlayers);
        direction = 1;
    }

    @Override
    protected TurnOrder _copy() {
        AlternatingTurnOrder to = new AlternatingTurnOrder();
        to.direction = direction;
        return to;
    }

    @Override
    public void _endRound(AbstractGameState gameState) {
        // do nothing
    }
    @Override
    public void _startRound(AbstractGameState gameState) {
        // do nothing
    }

    @Override
    protected void _reset() {
        direction = 1;
    }

    @Override
    public int nextPlayer(AbstractGameState gameState) {
        return (nPlayers + turnOwner + direction) % nPlayers;
    }

    public void reverse(){
        direction *= -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlternatingTurnOrder)) return false;
        if (!super.equals(o)) return false;
        AlternatingTurnOrder that = (AlternatingTurnOrder) o;
        return direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), direction);
    }
}
