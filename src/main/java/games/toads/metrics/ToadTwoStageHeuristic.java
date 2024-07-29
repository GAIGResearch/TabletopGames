package games.toads.metrics;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;

public class ToadTwoStageHeuristic extends TunableParameters implements IStateHeuristic {

    IStateHeuristic roundOne;
    IStateHeuristic roundTwo;

    /**
     * This is a state value heuristic that uses a different function for each round
     */
    public ToadTwoStageHeuristic() {
        addTunableParameter("roundOne", IStateHeuristic.class);
        addTunableParameter("roundTwo", IStateHeuristic.class);
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (gs.getRoundCounter() == 0)
            return roundOne.evaluateState(gs, playerId);
        return roundTwo.evaluateState(gs, playerId);
    }

    @Override
    public double minValue() {
        return Math.min(roundOne.minValue(), roundTwo.minValue());
    }

    @Override
    public double maxValue() {
        return Math.max(roundOne.maxValue(), roundTwo.maxValue());
    }

    @Override
    protected AbstractParameters _copy() {
        return new ToadTwoStageHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    @Override
    public ToadTwoStageHeuristic instantiate() {
        return this;
    }

    @Override
    public void _reset() {
        roundOne = (IStateHeuristic) getParameterValue("roundOne");
        roundTwo = (IStateHeuristic) getParameterValue("roundTwo");
    }
}
