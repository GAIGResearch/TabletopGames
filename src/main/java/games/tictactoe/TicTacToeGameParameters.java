package games.tictactoe;

import core.AbstractParameters;
import evaluation.TunableParameters;

import java.util.*;

public class TicTacToeGameParameters extends TunableParameters {

    public int gridSize = 3;

    public TicTacToeGameParameters() {
        this(0);
    }

    public TicTacToeGameParameters(long seed) {
        super(seed);
        addTunableParameter("gridSize", 3, Arrays.asList(3, 4, 5, 6));
        _reset();
    }

    @Override
    public void _reset() {
        gridSize = (int) getParameterValue("gridSize");
    }

    @Override
    protected AbstractParameters _copy() {
        TicTacToeGameParameters gp = new TicTacToeGameParameters(getRandomSeed());
        gp.gridSize = gridSize;
        return gp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TicTacToeGameParameters that = (TicTacToeGameParameters) o;
        return gridSize == that.gridSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridSize);
    }

    @Override
    public TicTacToeGame instantiate() {
        return new TicTacToeGame(this);
    }

}
