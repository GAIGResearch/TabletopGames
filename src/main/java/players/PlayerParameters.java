package players;

import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;

import java.util.*;

public abstract class PlayerParameters extends TunableParameters {

    // Budget settings
    public PlayerConstants budgetType = PlayerConstants.BUDGET_FM_CALLS;
    public int budget = 4000;
    public int breakMS = 10;

    // Heuristic
    public IStateHeuristic gameHeuristic;

    public PlayerParameters(long seed) {
        super(seed);
        addTunableParameter("budgetType", PlayerConstants.BUDGET_FM_CALLS);
        addTunableParameter("budget", 4000);
        addTunableParameter("breakMS", 10);
    }

    @Override
    public void _reset() {
        budget = (int) getParameterValue("budget");
        breakMS = (int) getParameterValue("breakMS");
        budgetType = (PlayerConstants) getParameterValue("budgetType");
    }

    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerParameters)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), budgetType, budget, gameHeuristic);
    }

}
