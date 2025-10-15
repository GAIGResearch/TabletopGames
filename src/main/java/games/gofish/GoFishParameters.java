package games.gofish;

import core.AbstractParameters;

public class GoFishParameters extends AbstractParameters {

    // Game-specific parameters
    public int startingHandSize = 5;

    public GoFishParameters() {
        super();
    }

    @Override
    protected GoFishParameters _copy() {
        GoFishParameters copy = new GoFishParameters();
        copy.startingHandSize = this.startingHandSize;
        return copy;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoFishParameters)) return false;
        GoFishParameters that = (GoFishParameters) o;
        return this.startingHandSize == that.startingHandSize;
    }

    // Custom method for locating assets
    public String getDataPath() {
        return "data/gofish/";
    }
}
