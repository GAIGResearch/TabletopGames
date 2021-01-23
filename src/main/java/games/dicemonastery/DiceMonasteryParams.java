package games.dicemonastery;

import core.AbstractParameters;

import java.util.Objects;

public class DiceMonasteryParams extends AbstractParameters {
    public DiceMonasteryParams(long seed) {
        super(seed);
    }

    public final int YEARS = 3;

    @Override
    protected DiceMonasteryParams _copy() {
        return new DiceMonasteryParams(System.currentTimeMillis());
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiceMonasteryParams) {
            DiceMonasteryParams other = (DiceMonasteryParams) o;
            return other.YEARS == YEARS;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(YEARS);
    }
}
