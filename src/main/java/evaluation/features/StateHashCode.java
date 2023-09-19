package evaluation.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class StateHashCode implements IStateFeatureVector {
    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        return new double[]{state.hashCode()};
    }

    @Override
    public String[] names() {
        return new String[] {"Hashcode"};
    }
}
