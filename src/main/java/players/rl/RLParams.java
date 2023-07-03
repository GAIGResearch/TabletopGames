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
    public RLType type;
    public final TabularParams tabular;

    // Only used for instance() method
    public String inFileNameOrAbsPath = null;

    // Tunable parameters
    public double epsilon = 0.25f;

    public RLParams(RLFeatureVector features, RLType type) {
        this(features, type, System.currentTimeMillis());
    }

    public RLParams(RLFeatureVector features, RLType type, long seed) {
        super(seed);
        this.features = features;
        this.type = type;
        tabular = this.type == RLType.Tabular ? new TabularParams() : null;
        addTunableParameters();
    }

    private void addTunableParameters() {
        addTunableParameter("epsilon", 0.5f, Arrays.asList(0f, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, 1f));
    }

    @Override
    public RLPlayer instantiate() {
        if (inFileNameOrAbsPath == null)
            throw new IllegalArgumentException("The variable inFileNameOrAbsPath must be set for instantiation");
        return new RLPlayer(this, inFileNameOrAbsPath);
    }

    @Override
    protected AbstractParameters _copy() {
        RLParams retValue = new RLParams(features, type, System.currentTimeMillis());
        retValue.epsilon = epsilon;
        return retValue;
    }

}
