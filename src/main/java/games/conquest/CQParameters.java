package games.conquest;

import core.AbstractGameState;
import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class CQParameters extends AbstractParameters {
    int gridWidth = 20;
    int gridHeight = 20;
    int setupPoints = 1000;
    int maxTroops = 10;
    int maxCommands = 4;
    int nSetupRows = 3; // setup allowed in first 3 rows only
    int randomSeed = 0;
    public static final String dataPath = "data/conquest/";

    public CQParameters() {
        setRandomSeed(randomSeed);
    }

    @Override
    protected AbstractParameters _copy() {
        // TODO: deep copy of all variables.
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        // TODO: compare all variables.
        return o instanceof CQParameters;
    }

    @Override
    public int hashCode() {
        // TODO: include the hashcode of all variables.
        return super.hashCode();
    }
}
