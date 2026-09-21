package games.agram;

import evaluation.optimisation.TunableParameters;
import games.tricktaking.ITrickTakingParameters;

import java.util.Arrays;

/**
 * Parameters for Agram (as described at https://www.pagat.com/last/agram.html).
 * All rule constants should be read from here rather than hard-coded in the state or forward model.
 */
public class AgramParameters extends TunableParameters<AgramParameters> implements ITrickTakingParameters {

    // Cards dealt to each player, and so the number of tricks played (Pagat: six; the Sink-Sink variant uses five)
    public int nCardsPerPlayer = 6;

    // Deals in a match. The winner of each deal deals the next (Pagat); the player with most deals won wins the match
    public int nDeals = 1;

    // Whether players remember the suits others have failed to follow (used when redeterminising)
    public boolean rememberVoids = true;

    public AgramParameters() {
        addTunableParameter("nCardsPerPlayer", 6, Arrays.asList(4, 5, 6));
        addTunableParameter("nDeals", 1, Arrays.asList(1, 3, 5));
        addTunableParameter("rememberVoids", true, Arrays.asList(false, true));
    }

    @Override
    public void _reset() {
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        nDeals = (int) getParameterValue("nDeals");
        rememberVoids = (boolean) getParameterValue("rememberVoids");
    }

    @Override
    public boolean rememberVoids() {
        return rememberVoids;
    }

    @Override
    protected AgramParameters _copy() {
        return new AgramParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof AgramParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public AgramParameters instantiate() {
        return this;
    }
}
