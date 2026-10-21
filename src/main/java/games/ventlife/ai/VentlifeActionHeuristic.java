package games.ventlife.ai;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.ventlife.VentlifeForwardModel;
import games.ventlife.VentlifeGameState;
import games.ventlife.actions.PlaceCreature;
import games.ventlife.actions.PlaceTile;
import games.ventlife.actions.StopPlacing;

import java.util.List;

/**
 * One-step look-ahead: the value of an action is the state heuristic's value (by default a VentlifeHeuristic), for
 * the player taking it, of the state it leads to.
 */
public class VentlifeActionHeuristic extends TunableParameters<VentlifeActionHeuristic> implements IActionHeuristic {

    private static final VentlifeForwardModel fm = new VentlifeForwardModel();
    IStateHeuristic heuristic = new VentlifeHeuristic();

    public VentlifeActionHeuristic() {
        addTunableParameter("heuristic", IStateHeuristic.class, new VentlifeHeuristic());
        _reset();
    }

    @Override
    public void _reset() {
        heuristic = (IStateHeuristic) getParameterValue("heuristic");
        if (heuristic == null)
            heuristic = new VentlifeHeuristic();
    }

    @Override
    public double evaluateAction(AbstractAction action, AbstractGameState state, List<AbstractAction> contextActions) {
        return evaluateAllActions(List.of(action), state)[0];
    }

    /**
     * Tiles are applied with VentlifeTileSim, and creature placements valued with VentlifeFastEval when the heuristic
     * allows it; anything else by copying the state and applying the action.
     */
    @Override
    public double[] evaluateAllActions(List<AbstractAction> actions, AbstractGameState gs) {
        VentlifeGameState state = (VentlifeGameState) gs;
        int player = state.getCurrentPlayer();
        double[] retValue = new double[actions.size()];
        VentlifeFastEval fast = null;
        VentlifeTileEval tileEval = null;
        if (heuristic instanceof VentlifeHeuristic h && h.fishSeed == 0 && actions.get(0) instanceof PlaceTile)
            tileEval = new VentlifeTileEval(state, player, h);
        if (heuristic instanceof VentlifeHeuristic h && h.fishSeed == 0
                && actions.stream().anyMatch(a -> a instanceof PlaceCreature))
            fast = new VentlifeFastEval(state, player, h);
        for (int i = 0; i < retValue.length; i++) {
            AbstractAction a = actions.get(i);
            if (a instanceof PlaceTile t)
                retValue[i] = tileEval != null ? tileEval.tileValue(player, t.tile, t.smoker, t.orientation)
                        : heuristic.evaluateState(VentlifeTileSim.afterTile(state, t), player);
            else if (fast != null && a instanceof PlaceCreature pc)
                retValue[i] = fast.placementValue(player, pc.species, pc.hex, pc.count);
            else if (fast != null && a instanceof StopPlacing)
                retValue[i] = fast.value(player);
            else {
                AbstractGameState next = state.copy();
                fm.next(next, a.copy());
                retValue[i] = heuristic.evaluateState(next, player);
            }
        }
        return retValue;
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
