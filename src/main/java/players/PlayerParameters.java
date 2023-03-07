package players;

import core.actions.ActionSpaceType;
import core.interfaces.IStateHeuristic;
import evaluation.TunableParameters;

import java.util.*;

public class PlayerParameters extends TunableParameters {

    public double exploreEpsilon;

    // Budget settings
    public PlayerConstants budgetType = PlayerConstants.BUDGET_FM_CALLS;
    public int budget = 4000;
    public int breakMS = 10;

    // Heuristic
    public IStateHeuristic gameHeuristic;

    // Action space type for this player
    public ActionSpaceType actionSpaceType = ActionSpaceType.Default;

    public PlayerParameters(long seed) {
        super(seed);
        addTunableParameter("budgetType", PlayerConstants.BUDGET_FM_CALLS, Arrays.asList(PlayerConstants.values()));
        addTunableParameter("budget", 4000, Arrays.asList(100, 300, 1000, 3000, 10000, 30000, 100000));
        addTunableParameter("breakMS", 10);
        addTunableParameter("actionSpaceType", ActionSpaceType.Default, Arrays.asList(ActionSpaceType.values()));
    }

    @Override
    protected PlayerParameters _copy() {
        PlayerParameters params = new PlayerParameters(getRandomSeed());
        params.exploreEpsilon = exploreEpsilon;
        params.budgetType = budgetType;
        params.budget = budget;
        params.breakMS = breakMS;
        params.gameHeuristic = gameHeuristic;
        params.actionSpaceType = actionSpaceType;
        return null;
    }

    @Override
    public void _reset() {
        budget = (int) getParameterValue("budget");
        breakMS = (int) getParameterValue("breakMS");
        budgetType = (PlayerConstants) getParameterValue("budgetType");
        actionSpaceType = (ActionSpaceType) getParameterValue("actionSpaceType");
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerParameters)) return false;
        if (!super.equals(o)) return false;
        PlayerParameters that = (PlayerParameters) o;
        return Double.compare(that.exploreEpsilon, exploreEpsilon) == 0 && budget == that.budget && breakMS == that.breakMS && budgetType == that.budgetType && Objects.equals(gameHeuristic, that.gameHeuristic) && actionSpaceType == that.actionSpaceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), exploreEpsilon, budgetType, budget, breakMS, gameHeuristic, actionSpaceType);
    }

    @Override
    public Object instantiate() {
        return null;
    }
}
