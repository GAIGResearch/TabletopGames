package games.battlelore;

import core.AbstractParameters;
import games.tictactoe.TicTacToeGame;
import games.tictactoe.TicTacToeGameParameters;

import java.util.Objects;

public class BattleloreGameParameters extends AbstractParameters
{
    String dataPath;

    public int hexWidth = 12; //A..L in odd numbers, A..K in even numbers.
    public int hexHeight = 9; //1-9

    public BattleloreGameParameters(String dataPath, long seed)
    {
        super(seed);
        this.dataPath = dataPath;

    }

    @Override
    protected AbstractParameters _copy()
    {
        BattleloreGameParameters copy = new BattleloreGameParameters(dataPath, System.currentTimeMillis());
        copy.hexWidth = hexWidth;
        copy.hexHeight = hexHeight;
        return copy;
    }

    public String getDataPath(){return dataPath;}

    @Override
    protected boolean _equals(Object o)
    {
        if (this == o) { return true; }
        if (!(o instanceof BattleloreGameParameters)) { return false; }
        if (super.equals(o)) return false;

        BattleloreGameParameters that = (BattleloreGameParameters) o;
        return hexWidth == that.hexWidth &&
                hexHeight == that.hexHeight &&
                dataPath == that.dataPath;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), hexWidth, hexHeight, dataPath);
    }

}