package players.rmhc;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import players.PlayerParameters;
import java.util.*;


public class RMHCParams extends PlayerParameters {
    public int horizon = 10;
    public double discountFactor = 0.9;
    public IStateHeuristic heuristic = AbstractGameState::getHeuristicScore;

    public RMHCParams() {
        addTunableParameter("horizon", 10, Arrays.asList(1, 3, 5, 10, 20, 30));
        addTunableParameter("discountFactor", 0.9, Arrays.asList(0.5, 0.8, 0.9, 0.95, 0.99, 0.999, 1.0));
        addTunableParameter("heuristic", (IStateHeuristic) AbstractGameState::getHeuristicScore);
    }

    @Override
    public void _reset() {
        super._reset();
        horizon = (int) getParameterValue("horizon");
        discountFactor = (double) getParameterValue("discountFactor");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
    }

    @Override
    protected RMHCParams _copy() {
        RMHCParams copy = new RMHCParams();
        copy.horizon = horizon;
        copy.discountFactor = discountFactor;
        return copy;
    }

    @Override
    public IStateHeuristic getStateHeuristic() {
        return heuristic;
    }

    @Override
    public RMHCPlayer instantiate() {
        return new RMHCPlayer(this);
    }
}
