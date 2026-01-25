package games.catan;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import players.heuristics.LinearStateHeuristic;
import players.heuristics.ScoreHeuristic;

public class MultiStageHeuristic extends TunableParameters implements IStateHeuristic {

    LinearStateHeuristic base, opening, early, late;
    CatanStateFeatures features = new CatanStateFeatures();
    int openingIndex = 35;
    int earlyIndex = 36;
    int lateIndex = 37;


    /**
     * This is a state value heuristic that uses a different function for each round
     */
    public MultiStageHeuristic() {
        // This is hardcoded for the moment to always use a linear state heuristic for a Score Plus based heuristic
        // the parameters then define the different coefficient files to use at different stages of the game

        addTunableParameter("base", "");
        addTunableParameter("opening", "");
        addTunableParameter("early", "");
        addTunableParameter("late", "");
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
        double[] phi = features.doubleVector(gs, playerId);
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
        return new MultiStageHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    @Override
    public MultiStageHeuristic instantiate() {
        return this;
    }

    @Override
    public void _reset() {
        String baseCoeff = (String) getParameterValue("base");
        String openingCoeff = (String) getParameterValue("opening");
        String earlyCoeff = (String) getParameterValue("early");
        String lateCoeff = (String) getParameterValue("late");
        base = new LinearStateHeuristic(features, baseCoeff, new ScoreHeuristic());
        if (openingCoeff.isEmpty())
            opening = base;
        else
            opening = new LinearStateHeuristic(features, openingCoeff, new ScoreHeuristic());
        if (earlyCoeff.isEmpty())
            early = base;
        else
            early = new LinearStateHeuristic(features, earlyCoeff, new ScoreHeuristic());
        if (lateCoeff.isEmpty())
            late = base;
        else
            late = new LinearStateHeuristic(features, lateCoeff, new ScoreHeuristic());
        if (opening == null || early == null || late == null)
            throw new AssertionError("At least base heuristic must be provided");
    }
}
