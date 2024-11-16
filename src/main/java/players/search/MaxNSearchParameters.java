package players.search;

import core.interfaces.IStateHeuristic;
import players.PlayerConstants;
import players.PlayerParameters;
import players.heuristics.GameDefaultHeuristic;

public class MaxNSearchParameters extends PlayerParameters {

    public enum SearchUnit {
        ACTION, MACRO_ACTION, TURN
    }

    protected int searchDepth = 1;
    protected SearchUnit searchUnit = SearchUnit.ACTION;
    protected IStateHeuristic heuristic;
    protected boolean paranoid = false;
    protected boolean iterativeDeepening = false;

    public MaxNSearchParameters() {
        this.addTunableParameter("searchDepth", 1);
        this.addTunableParameter("searchUnit", SearchUnit.ACTION);
        this.addTunableParameter("heuristic", IStateHeuristic.class);
        this.addTunableParameter("paranoid", false);
        this.addTunableParameter("iterativeDeepening", false);
    }

    @Override
    public void _reset() {
        super._reset();
        searchDepth = (int) getParameterValue("searchDepth");
        searchUnit = (SearchUnit) getParameterValue("searchUnit");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        paranoid = (boolean) getParameterValue("paranoid");
        iterativeDeepening = (boolean) getParameterValue("iterativeDeepening");
        if (heuristic == null) {
            heuristic = new GameDefaultHeuristic();
        }
        if (budgetType != PlayerConstants.BUDGET_TIME) {
            System.out.println("Warning: SearchPlayer only supports time-based budget limits. Setting to BUDGET_TIME.");
            budgetType = PlayerConstants.BUDGET_TIME;
        }
    }

    @Override
    protected MaxNSearchParameters _copy() {
        return new MaxNSearchParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof MaxNSearchParameters;
    }

    @Override
    public Object instantiate() {
        return new MaxNSearchPlayer(this);
    }

}
