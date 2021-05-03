package players.rmhc;

import core.AbstractParameters;
import players.PlayerParameters;
import java.util.*;


public class RMHCParams extends PlayerParameters {
    public int horizon = 10;
    public double discountFactor = 0.9;

    public RMHCParams() {
        this(System.currentTimeMillis());
    }

    public RMHCParams(long seed) {
        super(seed);
        addTunableParameter("horizon", 10, Arrays.asList(1, 3, 5, 10, 20, 30));
        addTunableParameter("discountFactor", 0.9, Arrays.asList(0.5, 0.8, 0.9, 0.95, 0.99, 0.999, 1.0));
    }

    @Override
    public void _reset() {
        super._reset();
        horizon = (int) getParameterValue("horizon");
        discountFactor = (double) getParameterValue("discountFactor");
    }

    @Override
    protected AbstractParameters _copy() {
        return new RMHCParams(System.currentTimeMillis());
    }


    @Override
    public RMHCPlayer instantiate() {
        return new RMHCPlayer(this);
    }
}
