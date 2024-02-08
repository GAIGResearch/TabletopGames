package games.connect4;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.*;

public class Connect4GameParameters extends TunableParameters {


    public int gridSize = 8;
    public int winCount = 4;

    public Connect4GameParameters() {
        addTunableParameter("gridSize", 8, Arrays.asList(6, 8, 10, 12));
        addTunableParameter("winCount", 4, Arrays.asList(3, 4, 5, 6));
        _reset();
    }

    @Override
    public void _reset() {
        gridSize = (int) getParameterValue("gridSize");
        winCount = (int) getParameterValue("winCount");
    }

    @Override
    protected AbstractParameters _copy() {
        Connect4GameParameters gp = new Connect4GameParameters();
        gp.gridSize = gridSize;
        gp.winCount = winCount;
        return gp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Connect4GameParameters that = (Connect4GameParameters) o;
        return gridSize == that.gridSize && winCount == that.winCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridSize, winCount);
    }

    @Override
    public Object instantiate() {
        return GameType.Connect4.createGameInstance(2, this);
    }


}
