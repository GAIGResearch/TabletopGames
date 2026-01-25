package core.interfaces;

import core.AbstractGameState;

import java.util.Arrays;

/**
 * This defines an interface for a feature vector representation of a game state.
 * It provides two access points to the underlying feature vector:
 * 1. A double array representation, which is useful for numeric algorithms that require high
 *    performance.
 * 2. An Object array representation, for when the features are not all numeric.
 *
 * Any specific implementation of this interface should provide a concrete implementation of
 * at least one of these methods. The other should then throw a UnsupportedOperationException.
 *
 * This is kept as a single interface to reduce overall complexity of the system, and because
 * the two methods can share helper methods.
 */
public interface IStateFeatureVector extends IStateKey {

    default double[] doubleVector(AbstractGameState state, int playerID) {
        double[] retValue = new double[names().length];
        Object[] features = featureVector(state, playerID);
        if (features.length == 0) {
            throw new UnsupportedOperationException("Feature vector is empty");
        }
        for (int i = 0; i < features.length; i++) {
            if (features[i] instanceof Number) {
                retValue[i] = ((Number) features[i]).doubleValue();
            } else {
                throw new UnsupportedOperationException("Feature " + names()[i] + " is not numeric");
            }
        }
        return retValue;
    }

    default Object[] featureVector(AbstractGameState state, int playerID) {
            double[] retValue = doubleVector(state, playerID);
            Object[] retObject = new Object[names().length];
            if (retValue.length == 0) {
                throw new UnsupportedOperationException("Feature vector is empty");
            }
            for (int i = 0; i < retObject.length; i++) {
                retObject[i] = retValue[i];
            }
            return retObject;
    }

    String[] names();

    default Class<?>[] types() {
        // the default is all double
        Class<?>[] retValue = new Class[names().length];
        Arrays.fill(retValue, Double.class);
        return retValue;
    }

    @Override
    default Integer getKey(AbstractGameState state, int p) {
        int retValue = state.getCurrentPlayer();
        try {
            double[] features = doubleVector(state, p);
            for (int i = 0; i < features.length; i++) {
                retValue += (int) (features[i] * Math.pow(31, i+1) - 1);
            }
        } catch (UnsupportedOperationException e) {
            // in this case we try the Object array
            Object[] features = featureVector(state, p);
            for (int i = 0; i < features.length; i++) {
                retValue += (int) (features[i].hashCode() * Math.pow(31, i+1) - 1);
            }
        }
        return retValue;
    }

}
