package evaluation.optimisation.ntbea.functions;

import evaluation.optimisation.ntbea.SearchSpace;
import evaluation.optimisation.ntbea.SolutionEvaluator;

import java.util.Random;

public class FunctionEvaluator implements SolutionEvaluator {

    Random rnd = new Random();
    int nEvals = 0;
    final NTBEAFunction fun;
    final SearchSpace searchSpace;


    public FunctionEvaluator(NTBEAFunction fun, SearchSpace searchSpace) {
        this.fun = fun;
        this.searchSpace = searchSpace;
    }

    @Override
    public void reset() {
        nEvals = 0;
    }

    @Override
    public double evaluate(int[] input) {
        nEvals++;
        return rnd.nextDouble() < actualBaseValue(input) ? 1.0 : 0.0;
    }

    public double actualBaseValue(int[] input) {
        // we now convert settings into the relevant values to feed to the function
        double[] settings = new double[fun.dimension()];
        for (int i = 0; i < settings.length; i++) {
            settings[i] = (double) searchSpace.value(i, input[i]);
        }
        return fun.functionValue(settings);
    }

    @Override
    public SearchSpace searchSpace() {
        return searchSpace;
    }
    @Override
    public int nEvals() {
        return nEvals;
    }
}

