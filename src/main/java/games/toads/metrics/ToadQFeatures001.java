package games.toads.metrics;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import core.interfaces.IActionHeuristic;

public class ToadQFeatures001 implements IActionFeatureVector {
    @Override
    public double[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        return new double[0];
    }

    @Override
    public String[] names() {
        return new String[0];
    }
}
