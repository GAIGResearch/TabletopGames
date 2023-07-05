package games.catan;

import core.AbstractGameState;
import players.heuristics.AbstractStateFeature;

public class CatanStateFeatures extends AbstractStateFeature {

    String[] localNames = new String[0];

    @Override
    protected double maxScore() {
        return 1.0;
    }

    @Override
    protected double maxRounds() {
        return 500.0;
    }

    @Override
    protected String[] localNames() {
        return localNames;
    }

    @Override
    protected double[] localFeatureVector(AbstractGameState gs, int playerID) {
        return new double[0];
    }

}
