package tools.descentTileBuild;

import core.AbstractParameters;

import java.util.Objects;

public class TileBuildParameters extends AbstractParameters {
    public int defaultGridSize = 5;
    public int maxGridSize = 50;

    public TileBuildParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        return new TileBuildParameters(System.currentTimeMillis());
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TileBuildParameters)) return false;
        if (!super.equals(o)) return false;
        TileBuildParameters that = (TileBuildParameters) o;
        return defaultGridSize == that.defaultGridSize &&
                maxGridSize == that.maxGridSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), defaultGridSize, maxGridSize);
    }
}
