package games.descent2e;

import core.AbstractParameters;

import java.util.Objects;

import static games.descent2e.DescentTypes.Campaign.HeirsOfBlood;

public class DescentParameters extends AbstractParameters {

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
    protected AbstractParameters _copy() {
        DescentParameters copy = new DescentParameters(System.currentTimeMillis());
        // TODO
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentParameters)) return false;
        if (!super.equals(o)) return false;
        DescentParameters that = (DescentParameters) o;
        return nActionsPerPlayer == that.nActionsPerPlayer &&
                pitFallHpCost == that.pitFallHpCost &&
                lavaHpCost == that.lavaHpCost &&
                waterMoveCost == that.waterMoveCost &&
                Objects.equals(dataPath, that.dataPath) &&
                campaign == that.campaign;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataPath, campaign, nActionsPerPlayer, pitFallHpCost, lavaHpCost, waterMoveCost);
    }
}
