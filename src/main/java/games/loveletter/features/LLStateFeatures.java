package games.loveletter.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class LLStateFeatures implements IStateFeatureVector {

    LLStateFeaturesTunable underlyingFeatures;
    public LLStateFeatures() {
        this.underlyingFeatures = new LLStateFeaturesTunable();
        // then set all active features to true
        for (String name : LLStateFeaturesTunable.allNames) {
            underlyingFeatures.setParameterValue(name, true);
        }
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        return underlyingFeatures.fullFeatureVector(state, playerID);
    }

    @Override
    public String[] names() {
        return LLStateFeaturesTunable.allNames;
    }
}
