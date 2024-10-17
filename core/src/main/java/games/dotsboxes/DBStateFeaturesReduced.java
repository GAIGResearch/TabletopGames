package games.dotsboxes;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class DBStateFeaturesReduced implements IStateFeatureVector {

    String[] names = new String[]{"POINT_ADVANTAGE", "POINTS", "THREE_BOXES", "TWO_BOXES", "ORDINAL"};

    private IStateFeatureVector fullFeatureVector = new DBStateFeatures();

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        double[] featureVector = fullFeatureVector.featureVector(state, playerID);
        double[] retValue = new double[names.length];
        retValue[0] = featureVector[1];
        retValue[1] = featureVector[0];
        retValue[2] = featureVector[8];
        retValue[3] = featureVector[7];
        retValue[4] = featureVector[2];
        return retValue;
    }

    @Override
    public String[] names() {
        return names;
    }
}
