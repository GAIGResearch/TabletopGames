package evaluation.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;

import java.util.Arrays;

public class StateKeyFromFeatureVector  implements IStateKey {

    public final IStateFeatureVector featureVector;

    public StateKeyFromFeatureVector(IStateFeatureVector featureVector) {
        this.featureVector = featureVector;
    }

    @Override
    public String getKey(AbstractGameState state, int playerId) {
        double[] retValue = featureVector.doubleVector(state, playerId);
        return String.format("%d-%s", playerId, Arrays.toString(retValue));
    }
}
