package games.battleship;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class BattleshipGameParameters extends TunableParameters {

    public int gridSize = 10;
    public int nShips = 5;
    public ArrayList<Integer> shipSizes = new ArrayList<>(5);

    public BattleshipGameParameters() {
        addTunableParameter("gridSize", 10, Arrays.asList(8, 10, 15, 20));
        addTunableParameter("nShips", 5, Arrays.asList(4, 5, 6));
        addTunableParameter("shipSizes", Arrays.asList(5, 4, 3, 3, 2), Arrays.asList(Arrays.asList(5, 4, 3, 3, 2), Arrays.asList(4, 3, 2, 2, 1), Arrays.asList(3, 3, 3, 3, 3)));
        _reset();
    }

    @Override
    protected AbstractParameters _copy() {
        BattleshipGameParameters gp = new BattleshipGameParameters();
        gp.gridSize = gridSize;
        gp.nShips = nShips;
        gp.shipSizes = (ArrayList<Integer>) shipSizes.clone();
        return gp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BattleshipGameParameters that = (BattleshipGameParameters) o;
        return gridSize == that.gridSize && nShips == that.nShips && Arrays.equals(shipSizes, that.shipSizes);
    }

    @Override
    public Object instantiate() {
        return GameType.Battleship.createGameInstance(2, this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridSize, nShips, Arrays.hashCode(shipSizes));
    }

    @Override
    public void _reset() {
        gridSize = (int) getParameterValue("gridSize");
        nShips = (int) getParameterValue("nShips");
        shipSizes = (int[]) getParameterValue("shipSizes");
    }
}
