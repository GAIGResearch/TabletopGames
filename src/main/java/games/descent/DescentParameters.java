package games.descent;

import core.AbstractGameParameters;

import static games.descent.DescentTypes.Campaign.HeirsOfBlood;

public class DescentParameters extends AbstractGameParameters {

    public String dataPath = "data/descent/";
    public DescentTypes.Campaign campaign = HeirsOfBlood;

    public int nActionsPerPlayer = 1;  // TODO: 2 move actions?
    public int pitFallHpCost = 2;
    public int lavaHpCost = 1;
    public int waterMoveCost = 2;

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
