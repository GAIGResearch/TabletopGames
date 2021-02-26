package games.sushigo;

import core.AbstractParameters;

public class SGParameters extends AbstractParameters {
    public SGParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        return null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}
