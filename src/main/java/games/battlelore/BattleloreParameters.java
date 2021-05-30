package games.battlelore;

import core.AbstractParameters;

public class BattleloreParameters extends AbstractParameters
{

    public int hexWidth = 12; //A..L in odd numbers, A..K in even numbers.
    public int hexHeight = 9; //1-9

    public BattleloreParameters(long seed)
    {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy()
    {
        BattleloreParameters copy = new BattleloreParameters(System.currentTimeMillis());
        copy.hexWidth = hexWidth;
        copy.hexHeight = hexHeight;
        return copy;
    }

    @Override
    protected boolean _equals(Object o)
    {
        return false;
    }
}
