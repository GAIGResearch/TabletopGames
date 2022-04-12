package core.interfaces;

import core.AbstractGameState;

public interface IStateFeatureVector<S extends AbstractGameState> {

    double[] featureVector(S state, int playerID);

}
