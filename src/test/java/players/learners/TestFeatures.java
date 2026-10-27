package players.learners;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class TestFeatures implements IStateFeatureVector {

    String[] names = new String[] {"X", "Y"};
    Class<?>[] types = new Class<?>[] {double.class, int.class};

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        return new double[] {0.0, 0.0};
    }

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public Class<?>[] types() {
        return types;
    }
}
