package games.descent;

import core.AbstractGameParameters;

public class DescentParameters extends AbstractGameParameters {

    String dataPath = "data/descent/";

    public DescentParameters(long seed) {
        super(seed);
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractGameParameters _copy() {
        DescentParameters copy = new DescentParameters(System.currentTimeMillis());
        // TODO
        return copy;
    }
}
