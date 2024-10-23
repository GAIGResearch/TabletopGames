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
public class CQParameters extends PlayerParameters {
    // Game parameters
    public final int gridWidth = 20;
    public final int gridHeight = 20;
    public final int setupPoints = 1000;
    public final int maxTroops = 10;
    public final int maxCommands = 4;
    public final int nSetupRows = 3; // setup allowed in first 3 rows only
    public static final String dataPath = "data/conquest/";
    // Player parameters
    public double K = Math.sqrt(2);
    public int maxTreeDepth = 10;
    public int rolloutLength = 10;
    public boolean paranoid = false;
    public int budget = 100;

    public CQParameters() {
        addTunableParameter("K", Math.sqrt(1.0), Arrays.asList(0.1, 1.0, 10.0));
        addTunableParameter("maxTreeDepth", 10, Arrays.asList(3, 10, 30));
        addTunableParameter("rolloutLength", 10, Arrays.asList(5, 10, 20));
        addTunableParameter("paranoid", false, Arrays.asList(false, true));
        addTunableParameter("budget", 100, Arrays.asList(100, 1000));
    }

    @Override
    public void _reset() {
        super._reset(); // This is important to ensure that PlayerParameters are also picked up (if being co-tuned)
        K = (double) getParameterValue("K");
        maxTreeDepth = (int) getParameterValue("maxTreeDepth");
        rolloutLength = (int) getParameterValue("rolloutLength");
        paranoid = (boolean) getParameterValue("openLoop");
        budget = (int) getParameterValue("budget");
    }

    @Override
    protected PlayerParameters _copy() {
        return new CQParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CQParameters;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Object instantiate() {
        return GameType.Conquest.createGameInstance(2, this);
    }
}
