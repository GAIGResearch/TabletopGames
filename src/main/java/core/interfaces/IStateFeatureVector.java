package core.interfaces;

import core.AbstractGameState;

public interface IStateFeatureVector extends IStateKey {

    double[] featureVector(AbstractGameState state, int playerID);

    String[] names();

    @Override
    default Integer getKey(AbstractGameState state, int p) {
        int retValue = state.getCurrentPlayer();
        double[] features = featureVector(state, p);
        for (int i = 0; i < features.length; i++) {
            retValue += (int) (features[i] * Math.pow(31, i+1) - 1);
        }
        return retValue;
    }

}
