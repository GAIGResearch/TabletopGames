package games.whist;

import evaluation.optimisation.TunableParameters;
import games.tricktaking.ITrickTakingParameters;

import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Whist (as described at https://www.pagat.com/whist/whist.html).
 */
public class WhistParameters extends TunableParameters<WhistParameters> implements ITrickTakingParameters {

    /**
     * How the trump suit of each deal is chosen.
     * TURN_UP: the dealer's last card is turned face up and its suit is trumps (pagat's standard rule).
     * ROTATION: trumps rotate by deal through Hearts, Diamonds, Spades and Clubs (and, with
     * WhistParameters.noTrumpsInRotation, a deal with no trumps), starting with Hearts.
     */
    public enum TrumpMode {TURN_UP, ROTATION}

    // Deals in the game. The side with more points after the last deal wins
    public int nDeals = 1;

    public TrumpMode trumpMode = TrumpMode.TURN_UP;

    // In ROTATION mode, a fifth deal in each cycle is played with no trumps
    public boolean noTrumpsInRotation = false;

    // Whether players remember the suits others have failed to follow (used when redeterminising)
    public boolean rememberVoids = true;

    public WhistParameters() {
        addTunableParameter("nDeals", 1, Arrays.asList(1, 2, 4, 8));
        addTunableParameter("trumpMode", TrumpMode.TURN_UP, List.of(TrumpMode.values()));
        addTunableParameter("noTrumpsInRotation", false, List.of(false, true));
        addTunableParameter("rememberVoids", true, List.of(false, true));
    }

    @Override
    public void _reset() {
        nDeals = (int) getParameterValue("nDeals");
        trumpMode = (TrumpMode) getParameterValue("trumpMode");
        noTrumpsInRotation = (boolean) getParameterValue("noTrumpsInRotation");
        rememberVoids = (boolean) getParameterValue("rememberVoids");
    }

    @Override
    public boolean rememberVoids() {
        return rememberVoids;
    }

    @Override
    protected WhistParameters _copy() {
        return new WhistParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof WhistParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public WhistParameters instantiate() {
        return this;
    }
}
