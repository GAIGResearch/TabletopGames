package games.santorini;

import core.AbstractParameters;

import java.util.Objects;

public class SantoriniParameters  extends AbstractParameters {

    int gridWidth = 5;
    int gridHeight = 5;

    public SantoriniParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        SantoriniParameters copy = new SantoriniParameters(System.currentTimeMillis());
        copy.gridWidth = gridWidth;
        copy.gridHeight = gridHeight;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SantoriniParameters that = (SantoriniParameters) o;
        return gridWidth == that.gridWidth &&
                gridHeight == that.gridHeight;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gridWidth, gridHeight);
    }
}