package core;

import core.interfaces.ITunableParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public abstract class AbstractGameParameters {

    // Random seed for the game
    long gameSeed;

    public AbstractGameParameters(long seed) {
        this.gameSeed = seed;
    }

    /* Methods to be implemented by subclass */

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     * @return - new game parameters object.
     */
    protected abstract AbstractGameParameters _copy();


    /* Public API */

    /**
     * Retrieve the random seed for this game.
     * @return - random seed.
     */
    public long getGameSeed() {
        return gameSeed;
    }

    /**
     * Copy this game parameter object.
     * @return - new object with the same parameters, but a new random seed.
     */
    public AbstractGameParameters copy() {
        AbstractGameParameters copy = _copy();
        copy.gameSeed = System.currentTimeMillis();
        return copy;
    }

    /**
     * Randomizes the set of parameters, if this is a class that implements the TunableParameters interface.
     */
    public void randomize() {
        if (this instanceof ITunableParameters) {
            Random rnd = new Random(gameSeed);
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
