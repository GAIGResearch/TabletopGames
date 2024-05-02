package core.interfaces;

import core.AbstractGameState;

public interface IStateFeatureVector {

    double[] featureVector(AbstractGameState state, int playerID);

    String[] names();

}
