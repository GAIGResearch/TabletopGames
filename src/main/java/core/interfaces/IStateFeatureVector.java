package core.interfaces;

import core.AbstractGameState;

import java.util.Arrays;

public interface IStateFeatureVector {

    double[] featureVector(AbstractGameState state, int playerID);
    String[] names();

    default int indexOf(String name) {
        return Arrays.asList(names()).indexOf(name);
    }

}
