package games.loveletter.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class LLStateFeatures implements IStateFeatureVector {

    LLStateFeaturesTunable underlyingFeatures;
    public LLStateFeatures() {
        this.underlyingFeatures = new LLStateFeaturesTunable();
        // then set all on
        for (String parameter : underlyingFeatures.names()) {
            underlyingFeatures.setParameterValue(parameter, true);
        }
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        return underlyingFeatures.doubleVector(state, playerID);
    }

    @Override
    public String[] names() {
        return underlyingFeatures.names();
    }
}
