package players;

import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;

import java.util.*;

public abstract class PlayerParameters extends TunableParameters {

    // Budget settings
    public PlayerConstants budgetType = PlayerConstants.BUDGET_FM_CALLS;
    public int iterationsBudget = 200;
    public int fmCallsBudget = 4000;
    public int timeBudget = 100; //milliseconds
    public int breakMS = 10;

    // Heuristic
    public IStateHeuristic gameHeuristic;

    public PlayerParameters(long seed) {
        super(seed);
 //       addTunableParameter("budgetType", PlayerConstants.BUDGET_FM_CALLS, Arrays.asList(PlayerConstants.values()));
        addTunableParameter("iterationsBudget", 200);
        addTunableParameter("fmCallsBudget", 4000);
        addTunableParameter("timeBudget", 100);
        addTunableParameter("breakMS", 10);
    }

    @Override
    public void _reset() {

    }

    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerParameters)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), budgetType, iterationsBudget, fmCallsBudget, timeBudget, gameHeuristic);
    }

}
