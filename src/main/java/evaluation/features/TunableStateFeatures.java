package evaluation.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateKey;
import evaluation.optimisation.TunableParameters;
import games.loveletter.features.LLStateFeaturesTunable;

import java.util.Arrays;
import java.util.stream.IntStream;

public abstract class TunableStateFeatures extends TunableParameters implements IStateFeatureVector, IStateKey {

    private final String[] allNames;

    protected boolean[] active;

    protected String[] namesUsed;


    // TODO: When extending this, the first thing to do is provide a list of the possible feature names in allNames
    public TunableStateFeatures(String[] allNames) {
        this.allNames = allNames;
        active = new boolean[allNames.length];
        Arrays.fill(active, true);
        for (String name : allNames) {
            addTunableParameter(name, true);
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
    public final double[] featureVector(AbstractGameState state, int playerID) {
        double[] data = fullFeatureVector(state, playerID);
        double[] retValue = new double[namesUsed.length];
        int count = 0;
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
     * @param state
     * @param playerID
     * @return
     */
    public abstract double[] fullFeatureVector(AbstractGameState state, int playerID);

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof TunableStateFeatures) {
            TunableStateFeatures other = (TunableStateFeatures) o;
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

    @Override
    public String getKey(AbstractGameState state) {
        double[] retValue = featureVector(state, state.getCurrentPlayer());
        return String.format("%d-%s", state.getCurrentPlayer(), Arrays.toString(retValue));
    }
}
