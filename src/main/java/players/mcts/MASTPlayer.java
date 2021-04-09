package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import utilities.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MASTPlayer extends AbstractPlayer {

    Random rnd;
    SingleTreeNode root;
    double temperature = 0.1;

    public MASTPlayer(Random rnd) {
        this.rnd = rnd;
    }

    public void setRoot(SingleTreeNode root) {
        this.root = root;
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        double[] pdf = new double[possibleActions.size()];
        for (int i = 0; i < possibleActions.size(); i++) {
            AbstractAction a = possibleActions.get(i);
            double actionValue = 0.0;
            if (root.MASTStatistics.containsKey(a)) {
                Pair<Integer, Double> stats = root.MASTStatistics.get(a);
                actionValue = stats.b / stats.a;
            }
            pdf[i] = actionValue;
        }

        double mean = Arrays.stream(pdf).sum() / possibleActions.size();
        for (int i = 0; i < pdf.length; i++)
            pdf[i] = Math.exp((pdf[i] - mean) / temperature);
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
}
