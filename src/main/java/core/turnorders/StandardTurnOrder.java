package core.turnorders;

public class StandardTurnOrder extends TurnOrder{

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
}
