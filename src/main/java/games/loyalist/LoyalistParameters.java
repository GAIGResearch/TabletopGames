package games.loyalist;

import core.AbstractParameters;

/** Five-player rules from Loyalist RULEBOOK_5P.md v0.6.9. */
public class LoyalistParameters extends AbstractParameters {
    /** Finite vocabulary for numeric speech; does not limit physical actions. */
    public int maximumReport = 30;

    public LoyalistParameters() {}

    public LoyalistParameters(long seed) {
        setRandomSeed(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        LoyalistParameters p = new LoyalistParameters();
        p.maximumReport = maximumReport;
        return p;
    }

    @Override
    protected boolean _equals(Object other) {
        return other instanceof LoyalistParameters p && maximumReport == p.maximumReport;
    }
}
