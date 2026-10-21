package games.ventlife.ai;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import evaluation.optimisation.TunableParameters;
import games.ventlife.VentlifeForwardModel;
import games.ventlife.VentlifeGameState;

import java.util.List;

/**
 * One-step look-ahead: the value of an action is the VentlifeHeuristic value, for the player taking it, of the state
 * it leads to. The heuristic's weights are this class's parameters.
 */
public class VentlifeActionHeuristic extends TunableParameters<VentlifeActionHeuristic> implements IActionHeuristic {

    private static final VentlifeForwardModel fm = new VentlifeForwardModel();
    double oppMaxWeight = 1.0;
    double oppMeanWeight = 0.0;

    public VentlifeActionHeuristic() {
        addTunableParameter("oppMaxWeight", 1.0);
        addTunableParameter("oppMeanWeight", 0.0);
        _reset();
    }

    @Override
    public void _reset() {
        oppMaxWeight = (double) getParameterValue("oppMaxWeight");
        oppMeanWeight = (double) getParameterValue("oppMeanWeight");
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        int player = state.getCurrentPlayer();
        VentlifeGameState next = (VentlifeGameState) state.copy();
        fm.next(next, action.copy());
        return VentlifeHeuristic.value(player, VentlifeScorer.scores(next), oppMaxWeight, oppMeanWeight);
    }

    @Override
    protected VentlifeActionHeuristic _copy() {
        return new VentlifeActionHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof VentlifeActionHeuristic;
    }

    @Override
    public VentlifeActionHeuristic instantiate() {
        return _copy();
    }
}
