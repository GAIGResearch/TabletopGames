package games.descent;

import core.AbstractGameParameters;

import static games.descent.DescentTypes.Campaign.HeirsOfBlood;

public class DescentParameters extends AbstractGameParameters {

    String dataPath = "data/descent/";
    DescentTypes.Campaign campaign = HeirsOfBlood;

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
