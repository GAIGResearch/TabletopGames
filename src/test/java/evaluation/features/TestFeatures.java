package evaluation.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class TestFeatures implements IStateFeatureVector {

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        return new double[] {0.0}; // never going to be used
    }

    @Override
    public String[] names() {
        return new String[] {"estateCount"};
    }
}
