package players.learners;

import evodef.SearchSpace;
import evodef.SolutionEvaluator;

public class SVMEvaluator implements SolutionEvaluator {
    int nEvals = 0;

    @Override
    public void reset() {
        nEvals = 0;
    }

    @Override
    public double evaluate(double[] solution) {
        throw new AssertionError("No need for implementation according to NTBEA library javadoc");
    }

    @Override
    public double evaluate(int[] solution) {
        return 0;
    }

    @Override
    public SearchSpace searchSpace() {
        return null;
    }

    @Override
    public int nEvals() {
        return 0;
    }
}
