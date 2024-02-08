package core.interfaces;

import core.AbstractGameState;

public interface IStateFeatureNormVector extends IStateFeatureVector {

    double[] normFeatureVector(AbstractGameState state, int playerID);

}
