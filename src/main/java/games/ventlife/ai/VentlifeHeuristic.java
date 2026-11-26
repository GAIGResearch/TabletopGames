package games.ventlife.ai;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.ventlife.VentlifeGameState;
import games.ventlife.components.Creature;
import games.ventlife.components.Hex;
import games.ventlife.components.Species;
import games.ventlife.components.Terrain;

import java.util.Map;

/**
 * The value of a state to a player: their adjusted score less oppMaxWeight times the best opponent's adjusted score
 * and oppMeanWeight times the mean of the opponents'. A finished game adds winBonus for a win.
 * <p>
 * The adjusted score is the game score, less the points the player's creatures on exposed positions stand to lose
 * (VentlifeScorer.exposedValue) times exposure (oppExposure for the opponents, if not negative), plus fishSeed for
 * each lone Fish that another Fish could still join. With endScaling the exposure fades out over the last round.
 */
public class VentlifeHeuristic extends TunableParameters<VentlifeHeuristic> implements IStateHeuristic {

    double oppMaxWeight = 1.0;
    double oppMeanWeight = 0.0;
    double winBonus = 0.0;
    double exposure = 0.0;
    double oppExposure = -1.0;
    double exposureMoveFactor = 0.5;
    double exposureCrabValue = 3.0;
    boolean endScaling = false;
    double fishSeed = 0.0;
    // the weight of the expected loss to covering (VentlifeScorer.riskValue), for the player and the opponents
    double risk = 0.0;
    double oppRisk = -1.0;
    // which CoverageModel to use
    int riskModel = 1;

    public VentlifeHeuristic() {
        addTunableParameter("oppMaxWeight", 1.0);
        addTunableParameter("oppMeanWeight", 0.0);
        addTunableParameter("winBonus", 0.0);
        addTunableParameter("exposure", 0.0);
        addTunableParameter("oppExposure", -1.0);
        addTunableParameter("exposureMoveFactor", 0.5);
        addTunableParameter("exposureCrabValue", 3.0);
        addTunableParameter("endScaling", false);
        addTunableParameter("fishSeed", 0.0);
        addTunableParameter("risk", 0.0);
        addTunableParameter("oppRisk", -1.0);
        addTunableParameter("riskModel", 1);
        _reset();
    }

    @Override
    public void _reset() {
        oppMaxWeight = (double) getParameterValue("oppMaxWeight");
        oppMeanWeight = (double) getParameterValue("oppMeanWeight");
        winBonus = (double) getParameterValue("winBonus");
        exposure = (double) getParameterValue("exposure");
        oppExposure = (double) getParameterValue("oppExposure");
        exposureMoveFactor = (double) getParameterValue("exposureMoveFactor");
        exposureCrabValue = (double) getParameterValue("exposureCrabValue");
        endScaling = (boolean) getParameterValue("endScaling");
        fishSeed = (double) getParameterValue("fishSeed");
        risk = (double) getParameterValue("risk");
        oppRisk = (double) getParameterValue("oppRisk");
        riskModel = (int) getParameterValue("riskModel");
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        VentlifeGameState state = (VentlifeGameState) gs;
        int[] scores = VentlifeScorer.scores(state);
        if (!state.isNotTerminal())
            return evaluate(state, playerId, scores);
        int n = scores.length;
        double[] adjusted = new double[n];
        for (int p = 0; p < n; p++)
            adjusted[p] = scores[p];
        double oppExp = oppExposure < 0 ? exposure : oppExposure;
        double oppR = oppRisk < 0 ? risk : oppRisk;
        if (exposure != 0 || oppExp != 0 || risk != 0 || oppR != 0) {
            Map<Hex, Integer> exposedPositions = VentlifeScorer.exposedPositions(state);
            if (exposure != 0 || oppExp != 0) {
                double scale = 1.0;
                if (endScaling) {
                    int remaining = state.getDrawDeck().getSize();
                    for (int p = 0; p < n; p++)
                        remaining += state.getHand(p).getSize();
                    scale = Math.min(1.0, (double) remaining / n);
                }
                double[] exposed = VentlifeScorer.exposedValue(state, exposedPositions, exposureMoveFactor, exposureCrabValue);
                for (int p = 0; p < n; p++)
                    adjusted[p] -= scale * (p == playerId ? exposure : oppExp) * exposed[p];
            }
            if (risk != 0 || oppR != 0) {
                double[] expected = VentlifeScorer.riskValue(state, exposedPositions, exposureMoveFactor, exposureCrabValue, riskModel);
                for (int p = 0; p < n; p++)
                    adjusted[p] -= (p == playerId ? risk : oppR) * expected[p];
            }
        }
        if (fishSeed != 0 && state.getSpeciesInPlay().contains(Species.EELPOUT_FISH))
            addFishSeeds(state, adjusted);
        return value(playerId, adjusted, oppMaxWeight, oppMeanWeight);
    }

    private void addFishSeeds(VentlifeGameState state, double[] adjusted) {
        Map<Hex, Creature> creatures = state.getCreatures();
        for (Map.Entry<Hex, Creature> e : creatures.entrySet()) {
            Creature c = e.getValue();
            if (c.species() != Species.EELPOUT_FISH || state.getSupply(c.owner(), Species.EELPOUT_FISH) == 0)
                continue;
            boolean alone = true, room = false;
            for (Hex nb : e.getKey().neighbours()) {
                Creature other = creatures.get(nb);
                if (other != null) {
                    if (other.species() == Species.EELPOUT_FISH && other.owner() == c.owner()) {
                        alone = false;
                        break;
                    }
                } else if (state.getCell(nb) != null && state.getCell(nb).terrain() != Terrain.BLACK_SMOKER)
                    room = true;
            }
            if (alone && room)
                adjusted[c.owner()] += fishSeed;
        }
    }

    /**
     * The value from precomputed scores (VentlifeScorer.scores), ignoring exposure.
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
        double[] d = new double[scores.length];
        for (int p = 0; p < scores.length; p++) d[p] = scores[p];
        return value(playerId, d, oppMaxWeight, oppMeanWeight);
    }

    public static double value(int playerId, double[] scores, double oppMaxWeight, double oppMeanWeight) {
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
