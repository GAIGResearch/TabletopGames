package core;

import core.interfaces.ITunableParameters;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;

public abstract class AbstractParameters {

    long randomSeed;

    public AbstractParameters(long seed) {
        randomSeed = seed;
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    protected abstract AbstractParameters _copy();

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    protected abstract boolean _equals(Object o);


    /* Public API */

    /**
     * Retrieve the random seed for this game.
     *
     * @return - random seed.
     */
    public long getRandomSeed() {
        return randomSeed;
    }

    /**
     * Copy this game parameter object.
     *
     * @return - new object with the same parameters, but a new random seed.
     */
    public AbstractParameters copy() {
        AbstractParameters copy = _copy();
        copy.randomSeed = System.currentTimeMillis();
        return copy;
    }

    /**
     * Randomizes the set of parameters, if this is a class that implements the TunableParameters interface.
     */
    public void randomize() {
        if (this instanceof ITunableParameters) {
            Random rnd = new Random(randomSeed);
            ITunableParameters params = (ITunableParameters) this;
            params.getParameterNames().forEach(name -> {
                        int nValues = params.getPossibleValues(name).size();
                        int randomChoice = rnd.nextInt(nValues);
                        params.setParameterValue(name, params.getPossibleValues(name).get(randomChoice));
                    }
            );
        } else {
            System.out.println("Error: Not implementing the TunableParameters interface. Not randomizing");
        }
    }

    /**
     * Resets the set of parameters to their default values, if this is a class that implements the TunableParameters
     * interface.
     */
    public void reset() {
        if (this instanceof ITunableParameters) {
            Map<String, Object> defaultValues = ((ITunableParameters) this).getDefaultParameterValues();
            ((ITunableParameters) this).setParameterValues(defaultValues);
        } else {
            System.out.println("Error: Not implementing the TunableParameters interface. Not resetting.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractParameters)) return false;
        AbstractParameters that = (AbstractParameters) o;
        return randomSeed == that.randomSeed && _equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(randomSeed);
    }
}
