package players;

import core.AbstractParameters;
import core.interfaces.IStateHeuristic;

import java.util.Objects;

public class PlayerParameters extends AbstractParameters {

    // Budget settings
    public int budgetType = PlayerConstants.BUDGET_FM_CALLS;
    public int iterationsBudget = 200;
    public int fmCallsBudget = 4000;
    public int timeBudget = 100; //milliseconds
    public long breakMS = 10;

    // Heuristic
    public IStateHeuristic gameHeuristic;

    public PlayerParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        return new PlayerParameters(System.currentTimeMillis());
    }

    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerParameters)) return false;
        if (!super.equals(o)) return false;
        PlayerParameters that = (PlayerParameters) o;
        return budgetType == that.budgetType &&
                iterationsBudget == that.iterationsBudget &&
                fmCallsBudget == that.fmCallsBudget &&
                timeBudget == that.timeBudget &&
                Objects.equals(gameHeuristic, that.gameHeuristic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), budgetType, iterationsBudget, fmCallsBudget, timeBudget, gameHeuristic);
    }

    @Override
    public AbstractParameters copy() {
        PlayerParameters copy = (PlayerParameters) _copy();
        copy.budgetType = budgetType;
        copy.iterationsBudget = iterationsBudget;
        copy.fmCallsBudget = fmCallsBudget;
        copy.timeBudget = timeBudget;
        copy.gameHeuristic = gameHeuristic;
        copy.randomSeed = System.currentTimeMillis();
        return copy;
    }
}
