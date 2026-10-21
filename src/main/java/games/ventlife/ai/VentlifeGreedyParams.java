package games.ventlife.ai;

import core.AbstractPlayer;
import players.PlayerConstants;
import players.PlayerParameters;

/**
 * Parameters of VentlifeGreedyPlayer. The budget is in milliseconds per decision.
 */
public class VentlifeGreedyParams extends PlayerParameters {

    // the opponents' scores in the value of a state (see VentlifeHeuristic)
    double oppMaxWeight = 1.0;
    double oppMeanWeight = 0.0;
    // how many of the best tile placements (by their one-step value) to play out to the end of the turn
    int tileLookahead = 10;

    public VentlifeGreedyParams() {
        addTunableParameter("oppMaxWeight", 1.0);
        addTunableParameter("oppMeanWeight", 0.0);
        addTunableParameter("tileLookahead", 10);
        setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
        setParameterValue("budget", 64);
    }

    @Override
    public void _reset() {
        super._reset();
        oppMaxWeight = (double) getParameterValue("oppMaxWeight");
        oppMeanWeight = (double) getParameterValue("oppMeanWeight");
        tileLookahead = (int) getParameterValue("tileLookahead");
    }

    @Override
    protected VentlifeGreedyParams _copy() {
        return new VentlifeGreedyParams();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof VentlifeGreedyParams;
    }

    @Override
    public AbstractPlayer instantiate() {
        return new VentlifeGreedyPlayer(this);
    }
}
