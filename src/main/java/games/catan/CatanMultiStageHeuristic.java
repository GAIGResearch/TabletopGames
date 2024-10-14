package games.catan;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

public class CatanMultiStageHeuristic extends TunableParameters implements IStateHeuristic {

    IStateHeuristic base, opening, early, late;
    CatanStateFeatures features = new CatanStateFeatures();
    int openingIndex = 35;
    int earlyIndex = 36;
    int lateIndex = 37;


    /**
     * This is a state value heuristic that uses a different function for each round
     */
    public CatanMultiStageHeuristic() {
        addTunableParameter("base", IStateHeuristic.class);
        addTunableParameter("opening", IStateHeuristic.class);
        addTunableParameter("early", IStateHeuristic.class);
        addTunableParameter("late", IStateHeuristic.class);
        if (!features.localNames[openingIndex].equals("OPENING_GAME")) {
            throw new AssertionError("Opening feature must be at index 35");
        }
        if (!features.localNames[earlyIndex].equals("EARLY_GAME")) {
            throw new AssertionError("Early feature must be at index 36");
        }
        if (!features.localNames[lateIndex].equals("LATE_GAME")) {
            throw new AssertionError("Late feature must be at index 37");
        }
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        double[] phi = features.featureVector(gs, playerId);
        if (phi[openingIndex] > 0.5)
            return opening.evaluateState(gs, playerId);
        if (phi[earlyIndex] > 0.5)
            return early.evaluateState(gs, playerId);
        if (phi[lateIndex] > 0.5)
            return late.evaluateState(gs, playerId);
        return base.evaluateState(gs, playerId);
    }

    @Override
    public double minValue() {
        return Math.min(base.minValue(), Math.min(opening.minValue(), Math.min(early.minValue(), late.minValue())));
    }

    @Override
    public double maxValue() {
        return Math.max(base.maxValue(), Math.max(opening.maxValue(), Math.max(early.maxValue(), late.maxValue())));
    }

    @Override
    protected AbstractParameters _copy() {
        return new CatanMultiStageHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    @Override
    public CatanMultiStageHeuristic instantiate() {
        return this;
    }

    @Override
    public void _reset() {
        base = (IStateHeuristic) getParameterValue("base");
        opening = (IStateHeuristic) getParameterValue("opening");
        early = (IStateHeuristic) getParameterValue("early");
        late = (IStateHeuristic) getParameterValue("late");
        if (opening == null) opening = base;
        if (early == null) early = base;
        if (late == null) late = base;
        if (opening == null || early == null || late == null) throw new AssertionError("At least base heuristic must be provided");
    }
}
