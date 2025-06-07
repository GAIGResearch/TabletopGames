package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class DBStateFeaturesReduced implements IStateFeatureVector {

    String[] names = new String[]{"POINT_ADVANTAGE", "POINTS", "THREE_BOXES", "TWO_BOXES", "ORDINAL", "TURN"};

    private IStateFeatureVector fullFeatureVector = new DBStateFeatures();

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        double[] featureVector = fullFeatureVector.doubleVector(state, playerID);
        double[] retValue = new double[names.length];
        retValue[0] = featureVector[5] - featureVector[4]; // Point advantage
        retValue[1] = featureVector[5];
        retValue[2] = featureVector[3];
        retValue[3] = featureVector[2];
        retValue[4] = state.getOrdinalPosition(playerID);
        retValue[5] = state.getTurnCounter();
        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }
}
