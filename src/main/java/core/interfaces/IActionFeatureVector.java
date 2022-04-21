package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface IActionFeatureVector {

    double[] featureVector(AbstractAction action, AbstractGameState state, int playerID);

    String[] names();

}
