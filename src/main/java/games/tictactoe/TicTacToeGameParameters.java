package games.tictactoe;

import core.AbstractParameters;
import evaluation.TunableParameters;

import java.util.*;

public class TicTacToeGameParameters extends TunableParameters {

    public static final String gridSize = "gridSize";
    public static final Map<String, Class<?>> types = new HashMap<>();

    public TicTacToeGameParameters(long seed) {
        super(seed);
        setParameterValue(gridSize, 3);
        types.put(gridSize, int.class);
    }

    @Override
    public void _reset() {
        // do nothing here as we use getParameter to access gridSize
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

    /**
     * Retrieve the types of all parameters.
     *
     */
    @Override
    public Map<String, Class<?>> getParameterTypes() {
        return types;
    }

    @Override
    public TicTacToeGame instantiate() {
        return new TicTacToeGame(this);
    }

}
