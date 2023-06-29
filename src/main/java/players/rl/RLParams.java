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
    public final String qWeightsFilePath;
    public final RLFeatureVector features;
    public final RLType type;
    public final TabularParams tabular;

    // Tunable parameters
    // TODO Choose good default values
    public float epsilon = 0.25f;

    public RLParams(String qWeightsFilePath, RLFeatureVector features, RLType type) {
        this(qWeightsFilePath, features, type, System.currentTimeMillis());
    }

    public RLParams(String qWeightsFilePath, RLFeatureVector features, RLType type, long seed) {
        super(seed);
        this.qWeightsFilePath = qWeightsFilePath;
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
        RLParams retValue = new RLParams(qWeightsFilePath, features, type, System.currentTimeMillis());
        retValue.epsilon = epsilon;
        return retValue;
    }

}
