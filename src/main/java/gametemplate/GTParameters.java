package gametemplate;

import core.AbstractParameters;

public class GTParameters extends AbstractParameters {
    public GTParameters(long seed) {
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
