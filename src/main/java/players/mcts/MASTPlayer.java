package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MASTPlayer extends AbstractPlayer {

    Random rnd;
    List<Map<AbstractAction, Pair<Integer, Double>>> MASTStatistics;
    double temperature = 0.1;

    public MASTPlayer(Random rnd) {
        this.rnd = rnd;
    }

    public void setStats(List<Map<AbstractAction, Pair<Integer, Double>>> MASTStatistics) {
        this.MASTStatistics = MASTStatistics;
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        double[] pdf = new double[possibleActions.size()];
        for (int i = 0; i < possibleActions.size(); i++) {
            AbstractAction a = possibleActions.get(i);
            double actionValue = 10.0; // arbitrary and non-zero
            int p = gameState.getCurrentPlayer();
            if (MASTStatistics.get(p).containsKey(a)) {
                Pair<Integer, Double> stats = MASTStatistics.get(p).get(a);
                actionValue = stats.a > 0 ? stats.b / stats.a : 0.0;
            }
            pdf[i] = actionValue;
        }

        double max = Arrays.stream(pdf).max().orElseThrow(() -> new AssertionError("Nothing in pdf!"));
        for (int i = 0; i < pdf.length; i++)
            pdf[i] = Math.exp((pdf[i] - max) / temperature);
        double sum = Arrays.stream(pdf).sum();
        for (int i = 0; i < pdf.length; i++)
            pdf[i] /= sum; // and normalise to a pdf

        // TODO: Use the same code here as in EXP3 and RM with a utility function?
        double cdfSample = rnd.nextDouble();
        double cdf = 0.0;
        for (int i = 0; i < pdf.length; i++) {
            cdf += pdf[i];
            if (cdf >= cdfSample)
                return possibleActions.get(i);
        }
        throw new AssertionError("If we reach here, then something has gone wrong in the above code");
    }

    @Override
    public MASTPlayer copy() {
        MASTPlayer retValue = new MASTPlayer(new Random(rnd.nextInt()));
        retValue.setStats(MASTStatistics);
        return retValue;
    }
}
