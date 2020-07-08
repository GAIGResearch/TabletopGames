package core;

import core.interfaces.ITunableParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public abstract class AbstractParameters {

    // Random seed for the game
    protected long randomSeed;

    public AbstractParameters(long seed) {
        this.randomSeed = seed;
    }

    /* Methods to be implemented by subclass */

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     * @return - new game parameters object.
     */
    protected abstract AbstractParameters _copy();


    /* Public API */

    /**
     * Retrieve the random seed for this game.
     * @return - random seed.
     */
    public long getRandomSeed() {
        return randomSeed;
    }

    /**
     * Copy this game parameter object.
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
            HashMap<Integer, ArrayList<?>> searchSpace = ((ITunableParameters)this).getSearchSpace();
            for (Map.Entry<Integer, ArrayList<?>> parameter: searchSpace.entrySet()) {
                int nValues = parameter.getValue().size();
                int randomChoice = rnd.nextInt(nValues);
                ((ITunableParameters)this).setParameterValue(parameter.getKey(), parameter.getValue().get(randomChoice));
            }
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
            HashMap<Integer, Object> defaultValues = ((ITunableParameters)this).getDefaultParameterValues();
            ((ITunableParameters)this).setParameterValues(defaultValues);
        } else {
            System.out.println("Error: Not implementing the TunableParameters interface. Not resetting.");
        }
    }
}
