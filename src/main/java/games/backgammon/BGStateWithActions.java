package games.backgammon;

import core.AbstractGameState;

public class BGStateWithActions extends BGStateFeatures {

    private BGForwardModel fm = new BGForwardModel();
    private final String[] allNames;

    public BGStateWithActions() {
        String[] baseNames = super.names();
        allNames = new String[baseNames.length + 1];
        System.arraycopy(baseNames, 0, allNames, 0, baseNames.length);
        allNames[allNames.length - 1] = "ActionSize";
    }

    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        double[] baseFeatures = super.doubleVector(state, playerID);
        double[] features = new double[baseFeatures.length + 1];
        // Copy base features
        System.arraycopy(baseFeatures, 0, features, 0, baseFeatures.length);
        // Add ActionSize
        features[features.length - 1] = fm.computeAvailableActions(state).size();
        return features;
    }

    @Override
    public String[] names() {
        return allNames;
    }
}
