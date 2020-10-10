package games.dotsboxes;

import core.AbstractParameters;

import java.util.Objects;

public class DBParameters extends AbstractParameters {
    int gridWidth = 7;
    int gridHeight = 5;

    public DBParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        DBParameters copy = new DBParameters(System.currentTimeMillis());
        copy.gridWidth = gridWidth;
        copy.gridHeight = gridHeight;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBParameters)) return false;
        if (!super.equals(o)) return false;
        DBParameters that = (DBParameters) o;
        return gridWidth == that.gridWidth &&
                gridHeight == that.gridHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridWidth, gridHeight);
    }
}
