package games.diamant;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.diamant.actions.ContinueInCave;

public class DiamantActionFeatures implements IActionFeatureVector {

    @Override
    public String[] names() {
        return new String[]{"IsExplore"};
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        if (action instanceof ContinueInCave)
            return new double[]{1.0};
        return new double[]{0.0};
    }
}
