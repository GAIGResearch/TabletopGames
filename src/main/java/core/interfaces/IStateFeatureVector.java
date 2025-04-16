package core.interfaces;

import core.AbstractGameState;
import jdk.jshell.spi.ExecutionControl;
import org.apache.commons.lang3.NotImplementedException;

/**
 * This defines an interface for a feature vector representation of a game state.
 * It provides two access points to the underlying feature vector:
 * 1. A double array representation, which is useful for numeric algorithms that require high
 *    performance.
 * 2. An Object array representation, for when the features are not all numeric.
 *
 * Any specific implementation of this interface should provide a concrete implementation of
 * at least one of these methods. The other should then throw a NotImplementedException.
 *
 * This is kept as a single interface to reduce overall complexity of the system, and because
 * the two methods can share helper methods.
 */
public interface IStateFeatureVector extends IStateKey {

    double[] doubleVector(AbstractGameState state, int playerID);

    Object[] featureVector(AbstractGameState state, int playerID);

    String[] names();

    @Override
    default Integer getKey(AbstractGameState state, int p) {
        int retValue = state.getCurrentPlayer();
        try {
            double[] features = doubleVector(state, p);
            for (int i = 0; i < features.length; i++) {
                retValue += (int) (features[i] * Math.pow(31, i+1) - 1);
            }
        } catch (NotImplementedException e) {
            // in this case we try the Object array
            Object[] features = featureVector(state, p);
            for (int i = 0; i < features.length; i++) {
                retValue += (int) (features[i].hashCode() * Math.pow(31, i+1) - 1);
            }
        }
        return retValue;
    }

}
