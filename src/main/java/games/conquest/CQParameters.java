package games.conquest;

import core.AbstractGameState;
import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import players.PlayerParameters;

import java.util.Arrays;

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
    // Game parameters
    public final int gridWidth = 20;
    public final int gridHeight = 20;
    public final int setupPoints = 1000;
    public final int maxTroops = 10;
    public final int maxCommands = 4;
    public final int nSetupRows = 3; // setup allowed in first 3 rows only
    public static final String dataPath = "data/conquest/";

    public CQParameters() {}

    @Override
    protected AbstractParameters _copy() {
        return this;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CQParameters;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
