package games.findmurderer;

import core.AbstractParameters;

import java.util.Objects;

public class MurderParameters extends AbstractParameters {
    int gridWidth = 10;  // Width of grid world
    int gridHeight = 10;  // Height of grid world
    double percPeopleOnGrid = 0.5;  // Percentage of the world that is filled randomly with people
    double percCivilianDeadWinKiller = 0.5;  // Percentage of the people in the world that have to be dead in order for the killer to win

    public MurderParameters(long seed) {
        super(seed);
    }

    @Override
    protected MurderParameters _copy() {
        MurderParameters mp = new MurderParameters(0);
        mp.gridHeight = gridHeight;
        mp.gridWidth = gridWidth;
        mp.percCivilianDeadWinKiller = percCivilianDeadWinKiller;
        mp.percPeopleOnGrid = percPeopleOnGrid;
        return mp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MurderParameters)) return false;
        if (!super.equals(o)) return false;
        MurderParameters that = (MurderParameters) o;
        return gridWidth == that.gridWidth && gridHeight == that.gridHeight && Double.compare(that.percPeopleOnGrid, percPeopleOnGrid) == 0 && Double.compare(that.percCivilianDeadWinKiller, percCivilianDeadWinKiller) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridWidth, gridHeight, percPeopleOnGrid, percCivilianDeadWinKiller);
    }
}
