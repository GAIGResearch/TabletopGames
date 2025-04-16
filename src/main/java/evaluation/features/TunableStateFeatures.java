package evaluation.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class TunableStateFeatures extends TunableParameters implements IStateFeatureVector {

    private final String[] allNames;

    protected boolean[] active;

    protected String[] namesUsed;


    /**
     *  When extending this, the first thing to do is provide a list of the possible feature names in allNames
      */
    public TunableStateFeatures(String[] allNames) {
        // we default to false. This means that automatic generation of the JSON
        // file after tuning shows the ones that are switched on (it only includes non-default values)
        this.allNames = allNames;
        active = new boolean[allNames.length];
        Arrays.fill(active, false);
        for (String name : allNames) {
            addTunableParameter(name, false);
        }
        _reset();
    }

    @Override
    public void _reset() {
        for (int i = 0; i < allNames.length; i++) {
            active[i] = (Boolean) getParameterValue(allNames[i]);
        }
        namesUsed = IntStream.range(0, allNames.length).filter(i -> active[i]).mapToObj(i -> allNames[i]).toArray(String[]::new);
    }

    @Override
    public final double[] doubleVector(AbstractGameState state, int playerID) {
        double[] data = fullFeatureVector(state, playerID);
        double[] retValue = new double[namesUsed.length + 1];
        int count = 1;
        retValue[0] = state.getCurrentPlayer();
        for (int i = 0; i < allNames.length; i++) {
            if (active[i]) {
                retValue[count] = data[i];
                count++;
            }
        }
        return retValue;
    }

    /**
     * Return the full feature vector, including all features, regardless of whether they are active or not.
     * @param state the state to provide the feature vector for
     * @param playerID the player from whose perspective the features are extracted
     * @return the full feature vector as a double array
     */
    public abstract double[] fullFeatureVector(AbstractGameState state, int playerID);

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof TunableStateFeatures other) {
            if (other.active.length != active.length)
                return false;
            for (int i = 0; i < active.length; i++) {
                if (other.active[i] != active[i])
                    return false;
            }
            return Arrays.equals(allNames, ((TunableStateFeatures) o).allNames);
        }
        return false;
    }

    @Override
    public TunableStateFeatures instantiate() {
        return (TunableStateFeatures) this._copy();
    }

    @Override
    public String[] names() {
        return namesUsed;
    }

}
