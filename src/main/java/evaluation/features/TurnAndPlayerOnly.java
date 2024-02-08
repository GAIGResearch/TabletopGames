package evaluation.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class TurnAndPlayerOnly implements IStateFeatureVector {
    @Override
    public String[] names() {
        return new String[]{"Player", "Round", "Turn"};
    }

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        return new double[]{state.getCurrentPlayer(), state.getRoundCounter(), state.getTurnCounter()};
    }

}
