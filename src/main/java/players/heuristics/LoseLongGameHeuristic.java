package players.heuristics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;


/**
 * A Heuristic to target the narrow loss of a long game.
 * weightForGameLength: how much to weight the game length in the heuristic (between 0 and 1)
 * approxMaxGameLength: the approximate maximum game length (used to normalise the game length component of the heuristic)
 * approxExpectedScore: the approximate expected score at the end of the game (used to normalise the score component of the heuristic)
 * targetWinDiff: the target win difference to aim for (used to normalise the score component of the heuristic)
 *
 * We reward long games (and penalise anything less than minGameLength).
 * We reward losing by a small margin (targetLossDiff) and penalise losing by a large margin or winning.
 *
 * The total heuristic value is:
 *     sigmoid((length - minGameLength) / CT) * weightForGameLength +
 *     sigmoid((targetLossDiff - lossDiff) / CS) * (1 - weightForGameLength) -
 *     winPenalty (if the game is won)
 *
 * CS, CT control the steepness of the sigmoid functions for the score and game length components, respectively.
 * They are calculated as scale * minGameLength and targetLossDiff respectively.
 */
public class LoseLongGameHeuristic extends TunableParameters<LoseLongGameHeuristic> implements IStateHeuristic {

    double weightForGameLength = 0.5;
    int minGameLength = 50;
    int targetLossDiff = 5;
    double winPenalty = 0.5;  // how much to penalise winning (between 0 and 1)
    double sigmoidScale = 0.2;  // how steep the sigmoid functions are (between 0 and 1)

    public LoseLongGameHeuristic() {
        addTunableParameter("weightForGameLength", 0.5);
        addTunableParameter("minGameLength", 50);
        addTunableParameter("targetLossDiff", 5);
        addTunableParameter("winPenalty", 0.5);
        addTunableParameter("sigmoidScale", 0.2);
    }

    private double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        int gameLength = gs.getGameTick();
        double lengthComponent = sigmoid((gameLength - minGameLength) / (sigmoidScale * minGameLength)) * weightForGameLength;

        double score = gs.getGameScore(playerId);
        double bestOpponentScore = Double.NEGATIVE_INFINITY;
        for (int p = 0; p < gs.getNPlayers(); p++) {
            if (p == playerId) continue;
            if (gs.getGameScore(p) > bestOpponentScore) {
                bestOpponentScore = gs.getGameScore(p);
            }
        }
        double lossDiff = bestOpponentScore - score;
        double scoreComponent = sigmoid((targetLossDiff - lossDiff) / (sigmoidScale * targetLossDiff));
        // at this stage, 0.5 means we are at the target loss diff, which needs to be 1.0, declining in either direction
        // so we scale
        scoreComponent = (1.0 - Math.abs(scoreComponent - 0.5) * 2) * (1 - weightForGameLength);

        boolean isWin = gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME;
        double winComponent = isWin ? winPenalty : 0;

        double finalValue =  lengthComponent + scoreComponent - winComponent;
        if (finalValue < 0.0) finalValue = 0.0;  // ensure we don't return negative values
        return finalValue;
    }

    @Override
    public LoseLongGameHeuristic instantiate() {
        return this._copy();
    }

    @Override
    public void _reset() {
        weightForGameLength = (double) getParameterValue("weightForGameLength");
        minGameLength = (int) getParameterValue("minGameLength");
        targetLossDiff = (int) getParameterValue("targetLossDiff");
        winPenalty = (double) getParameterValue("winPenalty");
        sigmoidScale = (double) getParameterValue("sigmoidScale");
    }

    @Override
    protected LoseLongGameHeuristic _copy() {
        return new LoseLongGameHeuristic();  // the value is then set in TunableParameters
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof LoseLongGameHeuristic;
    }

    @Override
    public String toString() {
        return "LoseLongGameHeuristic with weightForGameLength " + weightForGameLength +
                ", minGameLength " + minGameLength +
                ", targetLossDiff " + targetLossDiff +
                ", winPenalty " + winPenalty +
                ", sigmoidScale " + sigmoidScale;
    }

}
