package games.descent2e;

import core.AbstractParameters;
import games.descent2e.components.DiceType;

import java.util.HashMap;
import java.util.Objects;

import static games.descent2e.DescentTypes.Campaign.HeirsOfBlood;
import static games.descent2e.components.DiceType.RED;

public class DescentParameters extends AbstractParameters {

    public String dataPath = "data/descent2e/";
    public DescentTypes.Campaign campaign = HeirsOfBlood;

    public int nActionsPerFigure = 2;
    public HashMap<DiceType, Integer> reviveDice = new HashMap<DiceType, Integer>() {{
        put(RED, 2);
    }};

    public DescentParameters(long seed) {
        super(seed);
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        DescentParameters copy = new DescentParameters(System.currentTimeMillis());
        copy.nActionsPerFigure = nActionsPerFigure;
        copy.campaign = campaign;
        copy.dataPath = dataPath;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DescentParameters)) return false;
        if (!super.equals(o)) return false;
        DescentParameters that = (DescentParameters) o;
        return nActionsPerFigure == that.nActionsPerFigure &&
                Objects.equals(dataPath, that.dataPath) &&
                campaign == that.campaign;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataPath, campaign, nActionsPerFigure);
    }
}
