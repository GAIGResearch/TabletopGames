package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface IActionFeatureVector {

    double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID);

    default Object[] featureVector(AbstractAction action, AbstractGameState state, int playerID) {
        double[] doubleVector = doubleVector(action, state, playerID);
        Object[] retValue = new Object[doubleVector.length];
        for (int i = 0; i < doubleVector.length; i++) {
            retValue[i] = doubleVector[i];
        }
        return retValue;
    }

    String[] names();

}
