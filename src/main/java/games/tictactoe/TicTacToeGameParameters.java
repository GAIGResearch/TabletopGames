package games.tictactoe;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.*;

public class TicTacToeGameParameters extends TunableParameters {

    public int gridSize = 3;

    public TicTacToeGameParameters() {
        addTunableParameter("gridSize", 3, Arrays.asList(3, 4, 5, 6));
        _reset();
    }

    @Override
    public void _reset() {
        gridSize = (int) getParameterValue("gridSize");
    }

    @Override
    protected AbstractParameters _copy() {
        return new TicTacToeGameParameters();
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicTacToeGameParameters that = (TicTacToeGameParameters) o;
        return gridSize == that.gridSize;
    }

    @Override
    public Object instantiate() {
        return GameType.TicTacToe.createGameInstance(2, this);
    }

}
