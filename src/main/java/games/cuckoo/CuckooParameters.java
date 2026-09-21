package games.cuckoo;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Parameters for Cuckoo (as described at https://www.pagat.com/cuckoo/cuckoo.html).
 */
public class CuckooParameters extends TunableParameters<CuckooParameters> {

    // The lives each player starts with. A player with no lives left is out of the game
    public int nLives = 3;

    public CuckooParameters() {
        addTunableParameter("nLives", 3, Arrays.asList(1, 2, 3, 4, 5));
    }

    @Override
    public void _reset() {
        nLives = (int) getParameterValue("nLives");
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
