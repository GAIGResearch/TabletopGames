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
    public String getKey(AbstractGameState state) {
        double[] retValue = featureVector.featureVector(state, state.getCurrentPlayer());
        return String.format("%d-%s", state.getCurrentPlayer(), Arrays.toString(retValue));
    }
}
