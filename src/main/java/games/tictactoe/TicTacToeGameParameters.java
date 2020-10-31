package games.tictactoe;

import core.AbstractParameters;
import evaluation.TunableParameters;

import java.util.*;

public class TicTacToeGameParameters extends TunableParameters {

    public static final String gridSize = "gridSize";

    public TicTacToeGameParameters(long seed) {
        super(seed);
        setParameterValue(gridSize, 3);
    }

    @Override
    protected AbstractParameters _copy() {
        TicTacToeGameParameters tttgp = new TicTacToeGameParameters(System.currentTimeMillis());
        tttgp.setParameterValue(gridSize, getParameterValue(gridSize));
        return tttgp;
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
