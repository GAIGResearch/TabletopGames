package players;

import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;

import java.util.*;

public abstract class PlayerParameters extends TunableParameters {

    // Budget settings
    public PlayerConstants budgetType = PlayerConstants.BUDGET_ITERATIONS;
    public int iterationsBudget = 1000;
    public int fmCallsBudget = 4000;
    public int timeBudget = 100; //milliseconds
    public int breakMS = 10;

    // Heuristic
    public IStateHeuristic gameHeuristic;

    public PlayerParameters(long seed) {
        super(seed);
        addTunableParameter("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        addTunableParameter("iterationsBudget", 1000);
        addTunableParameter("fmCallsBudget", 4000);
        addTunableParameter("timeBudget", 100);
        addTunableParameter("breakMS", 10);
    }

    @Override
    public void _reset() {
        iterationsBudget = (int) getParameterValue("iterationsBudget");
        fmCallsBudget = (int) getParameterValue("fmCallsBudget");
        timeBudget = (int) getParameterValue("timeBudget");
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
        return Objects.hash(super.hashCode(), budgetType, iterationsBudget, fmCallsBudget, timeBudget, gameHeuristic);
    }

}
