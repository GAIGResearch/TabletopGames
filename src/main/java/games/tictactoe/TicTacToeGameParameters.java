package games.tictactoe;

import core.AbstractParameters;
import evaluation.TunableParameters;

import java.util.*;

public class TicTacToeGameParameters extends TunableParameters {

    public int gridSize = 3;

    public TicTacToeGameParameters(long seed) {
        super(seed);
        addTunableParameter("gridSize", 3, Arrays.asList(3, 4, 5, 6));
    }

    @Override
    public void _reset() {
        gridSize = (int) getParameterValue("gridSize");
    }

    @Override
    protected AbstractParameters _copy() {
        return new TicTacToeGameParameters(System.currentTimeMillis());
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TicTacToeGameParameters)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridSize);
    }


    @Override
    public String getParameterName(int parameterId) {
        if (parameterId == 0) return "Grid size";
        return null;
    }

    @Override
    public TicTacToeGame instantiate() {
        return new TicTacToeGame(this);
    }

}
