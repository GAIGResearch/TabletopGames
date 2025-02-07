package core;

import core.interfaces.IStateHeuristic;
import core.interfaces.ITunableParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import players.heuristics.NullHeuristic;

import java.util.*;

public abstract class AbstractParameters {

    // Random seed for this game
    long randomSeed;
    // Maximum number of rounds in the game - according to the rules
    // Once this is reached we end the game - and determine winners/losers in the normal way
    int maxRounds = -1;
    // Maximum number of rounds in the game before we timeout from boredom
    // If this is reached then we set the GameResult (and player results) to be TIMEOUT
    int timeoutRounds = -1;

    // Player thinking time for the entire game, in minutes. Default max value.
    long thinkingTimeMins = 90;
    // Increment in seconds, added after a common milestone: action, turn, round. Default 0.
    long incrementActionS = 0, incrementTurnS = 0, incrementRoundS = 0;
    // Increment in seconds, added after a custom milestone (to be added manually in game implementation). Default 0.
    long incrementMilestoneS = 0;


    public AbstractParameters() {
        this.setRandomSeed(System.currentTimeMillis());
    }

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     * It is important that this return a new object. As the super-class will amend the randomSeed
     * of the value returned.
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

    public void setMaxRounds(int max) {
        maxRounds = max;
    }
    public void setTimeoutRounds(int max) {
        timeoutRounds = max;
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
     * Retrieve the  maximum number of rounds before a game is terminated (According to the rules)
     * This is a valid end to a game, so winners/losers are determined as normal.
     *
     * @return - milestone increment
     */
    public int getMaxRounds() {
        return maxRounds;
    }
    /**
     * Retrieve the  maximum number of rounds before a game is terminated due to a 'timeout'
     * This is treated as an invalid end to the game, and the Game and all Player Results will
     * be set to TIMEOUT
     *
     * @return - milestone increment
     */
    public int getTimeoutRounds() {
        return timeoutRounds;
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
        if (this instanceof ITunableParameters<?> params) {
            Random rnd = new Random(randomSeed);
            params.getParameterNames().forEach(name -> {
                        int nValues = params.getPossibleValues(name).size();
                        int randomChoice = rnd.nextInt(nValues);
                        params.setParameterValue(name, params.getPossibleValues(name).get(randomChoice));
                    }
            );
            params._reset();
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
        if (!(o instanceof AbstractParameters that)) return false;
        return thinkingTimeMins == that.thinkingTimeMins &&
                incrementActionS == that.incrementActionS &&
                incrementTurnS == that.incrementTurnS &&
                incrementRoundS == that.incrementRoundS &&
                maxRounds == that.maxRounds && timeoutRounds == that.timeoutRounds &&
                incrementMilestoneS == that.incrementMilestoneS;
        // equals and hashcode deliberately excludes the random seed
    }

    @Override
    public int hashCode() {
        return Objects.hash(thinkingTimeMins, incrementActionS, incrementTurnS, incrementRoundS, incrementMilestoneS, maxRounds, timeoutRounds);
    }

    static public AbstractParameters createFromFile(GameType game, String fileName) {
        AbstractParameters params = game.createParameters(System.currentTimeMillis());
        if (fileName.isEmpty())
            return params;
        if (params instanceof TunableParameters) {
            TunableParameters.loadFromJSONFile((TunableParameters) params, fileName);
            return params;
        } else {
            throw new AssertionError("JSON parameter initialisation not supported for " + game);
        }
    }

    public IStateHeuristic getStateHeuristic() { return new NullHeuristic(); }
}
