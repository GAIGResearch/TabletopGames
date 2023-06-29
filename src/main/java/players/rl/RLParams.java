package players.rl;

import java.util.Arrays;

import core.AbstractParameters;
import players.PlayerParameters;
import players.rl.RLPlayer.RLType;

public class RLParams extends PlayerParameters {

    class TabularParams {
        public double unknownStateQValue = 0.5;
    }

    // Mandatory and automatic params
    public final RLFeatureVector features;
    public final RLType type;
    public final TabularParams tabular;

    // Tunable parameters
    public float epsilon = 0.25f;

    public RLParams(RLFeatureVector features, RLType type) {
        this(features, type, System.currentTimeMillis());
    }

    public RLParams(RLFeatureVector features, RLType type, long seed) {
        super(seed);
        this.features = features;
        this.type = type;
        tabular = this.type == RLType.TABULAR ? new TabularParams() : null;
        addTunableParameters();
    }

    private void addTunableParameters() {
        addTunableParameter("epsilon", 0.5f, Arrays.asList(0f, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, 1f));
    }

    @Override
    public RLPlayer instantiate() {
        return new RLPlayer(this);
    }

    @Override
    protected AbstractParameters _copy() {
        RLParams retValue = new RLParams(features, type, System.currentTimeMillis());
        retValue.epsilon = epsilon;
        return retValue;
    }

}
