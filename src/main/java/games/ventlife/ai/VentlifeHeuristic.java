package games.ventlife.ai;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.ventlife.VentlifeGameState;

/**
 * Own score less a weighted share of the opponents' scores: oppMaxWeight times the best opponent score plus
 * oppMeanWeight times their mean. A finished game adds winBonus for a win.
 */
public class VentlifeHeuristic extends TunableParameters<VentlifeHeuristic> implements IStateHeuristic {

    double oppMaxWeight = 1.0;
    double oppMeanWeight = 0.0;
    double winBonus = 0.0;

    public VentlifeHeuristic() {
        addTunableParameter("oppMaxWeight", 1.0);
        addTunableParameter("oppMeanWeight", 0.0);
        addTunableParameter("winBonus", 0.0);
        _reset();
    }

    @Override
    public void _reset() {
        oppMaxWeight = (double) getParameterValue("oppMaxWeight");
        oppMeanWeight = (double) getParameterValue("oppMeanWeight");
        winBonus = (double) getParameterValue("winBonus");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        return evaluate((VentlifeGameState) gs, playerId, VentlifeScorer.scores((VentlifeGameState) gs));
    }

    /**
     * The value from precomputed scores (VentlifeScorer.scores).
     */
    public double evaluate(VentlifeGameState state, int playerId, int[] scores) {
        double value = value(playerId, scores, oppMaxWeight, oppMeanWeight);
        if (winBonus != 0 && !state.isNotTerminal() && state.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            value += winBonus;
        return value;
    }

    /**
     * Own score less oppMaxWeight times the best opponent score and oppMeanWeight times the mean opponent score.
     */
    public static double value(int playerId, int[] scores, double oppMaxWeight, double oppMeanWeight) {
        double max = Double.NEGATIVE_INFINITY, total = 0;
        for (int p = 0; p < scores.length; p++) {
            if (p == playerId) continue;
            max = Math.max(max, scores[p]);
            total += scores[p];
        }
        return scores[playerId] - oppMaxWeight * max - oppMeanWeight * total / (scores.length - 1);
    }

    @Override
    protected VentlifeHeuristic _copy() {
        return new VentlifeHeuristic();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof VentlifeHeuristic;
    }

    @Override
    public VentlifeHeuristic instantiate() {
        return _copy();
    }

    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }
}
