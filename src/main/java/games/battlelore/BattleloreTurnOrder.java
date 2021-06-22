package games.battlelore;


import core.turnorders.TurnOrder;

public class BattleloreTurnOrder extends TurnOrder
{
    public BattleloreTurnOrder(int nPlayers)
    {
        super(nPlayers);
    }

    @Override
    protected void _reset()
    {

    }

    @Override
    protected TurnOrder _copy()
    {
        return new BattleloreTurnOrder(nPlayers);
    }
}
