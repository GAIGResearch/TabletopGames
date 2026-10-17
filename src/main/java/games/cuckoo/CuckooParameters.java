package games.cuckoo;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Parameters for Cuckoo (as described at https://www.pagat.com/cuckoo/cuckoo.html).
 * data/cuckoo/Cuckoo_Valet.json gives the RECYCLE version of the game: a single deal, which the player with the lowest
 * card loses.
 */
public class CuckooParameters extends TunableParameters<CuckooParameters> {

    // The lives each player starts with. A player with no lives left is out of the game
    public int nLives = 3;

    // If more than 0, the game ends after this many deals even with several players left. Those with the most lives
    // win (RECYCLE: 1 deal with 1 life each, so the players holding the lowest card lose and everyone else wins)
    public int maxDeals = 0;

    public CuckooParameters() {
        addTunableParameter("nLives", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("maxDeals", 0, Arrays.asList(0, 1, 5, 10));
        _reset();
    }

    @Override
    public void _reset() {
        nLives = (int) getParameterValue("nLives");
        maxDeals = (int) getParameterValue("maxDeals");
    }

    @Override
    protected CuckooParameters _copy() {
        return new CuckooParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CuckooParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public CuckooParameters instantiate() {
        return this;
    }
}
