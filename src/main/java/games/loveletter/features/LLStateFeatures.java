package games.loveletter.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class LLStateFeatures implements IStateFeatureVector {

    LLStateFeaturesTunable underlyingFeatures;
    public LLStateFeatures() {
        this.underlyingFeatures = new LLStateFeaturesTunable();
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
