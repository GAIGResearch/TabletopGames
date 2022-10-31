package core;

import core.interfaces.ITunableParameters;

import java.util.*;

public abstract class AbstractParameters {

    // Random seed for this game
    long randomSeed;

    // Player thinking time for the entire game, in minutes. Default max value.
    long thinkingTimeMins = 90;
    // Increment in seconds, added after a common milestone: action, turn, round. Default 0.
    long incrementActionS = 0, incrementTurnS = 0, incrementRoundS = 0;
    // Increment in seconds, added after a custom milestone (to be added manually in game implementation). Default 0.
    long incrementMilestoneS = 0;

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

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public void setThinkingTimeMins(long thinkingTimeMins) {
        this.thinkingTimeMins = thinkingTimeMins;
    }


    /**
     * Retrieve total thinking time for the game, in minutes
     *
     * @return - thinking time.
     */
    public long getThinkingTimeMins() {
        return thinkingTimeMins;
    }

    /**
     * Retrieve the number of seconds added to a player's timer after an action is taken
     *
     * @return - action increment
     */
    public long getIncrementActionS() {
        return incrementActionS;
    }

    /**
     * Retrieve the number of seconds added to a player's timer after a turn is finished
     *
     * @return - turn increment
     */
    public long getIncrementTurnS() {
        return incrementTurnS;
    }

    /**
     * Retrieve the number of seconds added to a player's timer after a round is finished
     *
     * @return - round increment
     */
    public long getIncrementRoundS() {
        return incrementRoundS;
    }

    /**
     * Retrieve the number of seconds added to a player's timer after a game-specific defined milestone
     *
     * @return - milestone increment
     */
    public long getIncrementMilestoneS() {
        return incrementMilestoneS;
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
        return thinkingTimeMins == that.thinkingTimeMins &&
                incrementActionS == that.incrementActionS &&
                incrementTurnS == that.incrementTurnS &&
                incrementRoundS == that.incrementRoundS &&
                incrementMilestoneS == that.incrementMilestoneS;
        // equals and hashcode deliberatley excludes the random seed
    }

    @Override
    public int hashCode() {
        return Objects.hash(thinkingTimeMins, incrementActionS, incrementTurnS, incrementRoundS, incrementMilestoneS);
    }
}
