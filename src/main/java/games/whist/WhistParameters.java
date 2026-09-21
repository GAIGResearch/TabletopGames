package games.whist;

import core.components.FrenchCard;
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

    // The trump suits of the ROTATION cycle, in order from the first deal
    public static final List<FrenchCard.Suite> TRUMP_ROTATION =
            List.of(FrenchCard.Suite.Hearts, FrenchCard.Suite.Diamonds, FrenchCard.Suite.Spades, FrenchCard.Suite.Clubs);

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

    /**
     * The trump suit of the given deal (from 0) in ROTATION mode, or null for a deal with no trumps.
     */
    public FrenchCard.Suite rotationTrumps(int deal) {
        int cycle = noTrumpsInRotation ? TRUMP_ROTATION.size() + 1 : TRUMP_ROTATION.size();
        int index = deal % cycle;
        return index < TRUMP_ROTATION.size() ? TRUMP_ROTATION.get(index) : null;
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
