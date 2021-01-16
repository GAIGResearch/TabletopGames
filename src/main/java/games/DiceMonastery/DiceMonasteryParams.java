package games.DiceMonastery;

import core.AbstractParameters;

public class DiceMonasteryParams extends AbstractParameters {
    public DiceMonasteryParams(long seed) {
        super(seed);
    }

    @Override
    protected DiceMonasteryParams _copy() {
        return null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
