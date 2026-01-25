package players.search;

import core.AbstractPlayer;
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
    protected IStateHeuristic heuristic = new GameDefaultHeuristic();
    protected boolean paranoid = false;
    protected boolean alphaBetaPruning = true;
    protected boolean iterativeDeepening = false;
    protected boolean expandByEstimatedValue = false;

    public MaxNSearchParameters() {
        this.addTunableParameter("searchDepth", 1);
        this.addTunableParameter("searchUnit", SearchUnit.ACTION);
        this.addTunableParameter("heuristic", IStateHeuristic.class);
        this.addTunableParameter("paranoid", false);
        this.addTunableParameter("iterativeDeepening", false);
        this.addTunableParameter("alphaBetaPruning", true);
        this.addTunableParameter("expandByEstimatedValue", false);
    }

    @Override
    public void _reset() {
        super._reset();
        searchDepth = (int) getParameterValue("searchDepth");
        searchUnit = (SearchUnit) getParameterValue("searchUnit");
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        paranoid = (boolean) getParameterValue("paranoid");
        iterativeDeepening = (boolean) getParameterValue("iterativeDeepening");
        alphaBetaPruning = (boolean) getParameterValue("alphaBetaPruning");
        expandByEstimatedValue = (boolean) getParameterValue("expandByEstimatedValue");
        if (heuristic == null) {
            heuristic = new GameDefaultHeuristic();
        }
        if (budgetType != PlayerConstants.BUDGET_TIME) {
      //      System.out.println("Warning: SearchPlayer only supports time-based budget limits. Setting to BUDGET_TIME.");
            budgetType = PlayerConstants.BUDGET_TIME;
        }
        if (expandByEstimatedValue && !alphaBetaPruning) {
            System.out.println("Warning: expandByEstimatedValue only makes sense with alphaBetaPruning. Disabling expandByEstimatedValue.");
            expandByEstimatedValue = false;
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
    public AbstractPlayer instantiate() {
        return new MaxNSearchPlayer(this);
    }

}
