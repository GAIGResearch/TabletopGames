package players.rl;

import java.util.Arrays;

import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import players.PlayerParameters;
import players.rl.RLPlayer.RLType;
import players.rl.utils.ApplyActionStateFeatureVector;

public class RLParams extends PlayerParameters {

    class TabularParams {
        public double unknownStateQValue = 0.5;
    }

    // Mandatory and automatic params
    public final IActionFeatureVector features;
    public RLType type;
    public final TabularParams tabular;

    // Only used for instance() method
    public String inFileNameOrAbsPath = null;

    // Tunable parameters
    public double epsilon = 0.25f;

    public RLParams(IActionFeatureVector features, RLType type) {
        this(features, type, System.currentTimeMillis());
    }

    public RLParams(IActionFeatureVector features, RLType type, long seed) {
        super(seed);
        this.features = features;
        this.type = type;
        tabular = this.type == RLType.Tabular ? new TabularParams() : null;
        addTunableParameters();
    }

    public RLParams(IStateFeatureVector features, RLType type) {
        this(features, type, System.currentTimeMillis());
    }

    public RLParams(IStateFeatureVector features, RLType type, long seed) {
        this(new ApplyActionStateFeatureVector(features), type, seed);
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
    protected RLParams _copy() {
        RLParams retValue = new RLParams(features, type, System.currentTimeMillis());
        retValue.epsilon = epsilon;
        return retValue;
    }

    // This should be used instead of this.features.getClass().getCanonicalName(),
    // since a feature vector extending IStateFeatureVector gets wrapped in another
    // class. If this.features.getClass().getCanonicalName() was used,the wrapper
    // class would be returned instead, wheras this prints the wrapped class instead
    public String getFeatureVectorCanonicalName() {
        if (features instanceof ApplyActionStateFeatureVector)
            return ((ApplyActionStateFeatureVector) features).getFeatureVectorCanonicalName();
        return features.getClass().getCanonicalName();
    }

}
