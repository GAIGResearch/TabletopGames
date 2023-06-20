package players.rl;

import java.util.Arrays;

import core.AbstractParameters;
import core.interfaces.IStateFeatureVector;
import players.PlayerParameters;

public class RLParams extends PlayerParameters {

    // TODO Choose good default values
    public float epsilon = 0.5f;

    public final IStateFeatureVector features;

    public RLParams(IStateFeatureVector features) {
        this(features, System.currentTimeMillis());
    }

    public RLParams(IStateFeatureVector features, long seed) {
        super(seed);
        this.features = features;
        addTunableParameter("epsilon", 0.5f, Arrays.asList(0f, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, 1f));
    }

    @Override
    public RLPlayer instantiate() {
        // TODO fix
        return null; // new AbstractRLPlayer(this);
    }

    @Override
    protected AbstractParameters _copy() {
        RLParams retValue = new RLParams(features, System.currentTimeMillis());
        retValue.epsilon = epsilon;
        return retValue;
    }

}
