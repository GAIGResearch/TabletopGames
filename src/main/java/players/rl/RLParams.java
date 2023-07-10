package players.rl;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;

import core.interfaces.IActionFeatureVector;
import core.interfaces.IStateFeatureVector;
import players.PlayerParameters;
import players.rl.DataProcessor.Field;
import players.rl.RLPlayer.RLType;
import players.rl.utils.ApplyActionStateFeatureVector;

public class RLParams extends PlayerParameters {

    class TabularParams {
        public double unknownStateQValue = 0.5;

        void copyFrom(TabularParams that) {
            this.unknownStateQValue = that.unknownStateQValue;
        }
    }

    private boolean initialized = false;

    // Mandatory and automatic params
    private TabularParams tabularParams;
    private RLType type;
    private IActionFeatureVector features;

    final String infileNameOrPath;

    // Tunable parameters
    public double epsilon = 0.25f;

    public RLParams(String infileNameOrPath) {
        this(infileNameOrPath, System.currentTimeMillis());
    }

    public RLParams(String infileNameOrPath, long seed) {
        super(seed);
        if (infileNameOrPath == null)
            throw new IllegalArgumentException("The input file name or path cannot be null");
        this.infileNameOrPath = infileNameOrPath;
        addTunableParameters();

    }

    RLParams(IStateFeatureVector features, RLType type, long seed) {
        this(new ApplyActionStateFeatureVector(features), type, seed);
    }

    RLParams(IActionFeatureVector features, RLType type, long seed) {
        super(seed);
        this.infileNameOrPath = null;
        this.features = features;
        this.type = type;
        tabularParams = this.type == RLType.Tabular ? new TabularParams() : null;
    }

    void initializeFromInfile(String gameName) {
        if (initialized == true)
            return;

        JsonNode metadata = DataProcessor.readInputFile(gameName, infileNameOrPath).get("Metadata");

        // Assign type from the input metadata
        type = RLType.valueOf(metadata.get(Field.Type.name()).asText());

        // Assign featureVector (long code since 2 possible data types)
        String featureVectorClassName = metadata.get(Field.FeatureVector.name()).asText();
        Object featureVectorInstance;
        try {
            featureVectorInstance = Class.forName(featureVectorClassName)
                    .getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }
        if (IActionFeatureVector.class.isAssignableFrom(featureVectorInstance.getClass()))
            features = (IActionFeatureVector) featureVectorInstance;
        else if (IStateFeatureVector.class.isAssignableFrom(featureVectorInstance.getClass()))
            features = new ApplyActionStateFeatureVector((IStateFeatureVector) featureVectorInstance);
        else
            throw new IllegalArgumentException(
                    "Feature Vector " + featureVectorClassName + " could not be initialized");

        initialized = true;
    }

    private void addTunableParameters() {
        addTunableParameter("epsilon", 0.5f, Arrays.asList(0f, .1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, 1f));
    }

    @Override
    public RLPlayer instantiate() {
        return new RLPlayer(this);
    }

    @Override
    protected RLParams _copy() {
        RLParams retValue = infileNameOrPath == null
                ? new RLParams(features, type, System.currentTimeMillis())
                : new RLParams(infileNameOrPath, System.currentTimeMillis());
        if (type == RLType.Tabular)
            retValue.getTabularParams().copyFrom(tabularParams);
        retValue.epsilon = epsilon;
        return retValue;
    }

    public RLType getType() {
        return type;
    }

    public IActionFeatureVector getFeatures() {
        return features;
    }

    public TabularParams getTabularParams() {
        return tabularParams;
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
