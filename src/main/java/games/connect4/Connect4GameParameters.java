package games.connect4;

import core.AbstractParameters;
import evaluation.TunableParameters;

import java.util.*;

public class Connect4GameParameters extends TunableParameters {


    public int gridSize = 8;

    public Connect4GameParameters() {
        this(System.currentTimeMillis());
    }

    public Connect4GameParameters(long seed) {
        super(seed);
        addTunableParameter("gridSize", 8, Arrays.asList(6, 8, 10, 12));
    }

    @Override
    public void _reset() {
        gridSize = (int) getParameterValue("gridSize");
    }

    @Override
    protected AbstractParameters _copy() {
        Connect4GameParameters gp = new Connect4GameParameters(System.currentTimeMillis());
        gp.gridSize = gridSize;
        return gp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Connect4GameParameters that = (Connect4GameParameters) o;
        return gridSize == that.gridSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridSize);
    }

    @Override
    public Connect4Game instantiate() {
        return new Connect4Game(this);
    }


}
