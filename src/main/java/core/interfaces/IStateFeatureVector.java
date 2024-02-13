package core.interfaces;

import core.AbstractGameState;

import java.util.Arrays;

public interface IStateFeatureVector {

    double[] featureVector(AbstractGameState state, int playerID);

    String[] names();

}
