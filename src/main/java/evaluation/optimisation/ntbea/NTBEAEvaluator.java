package evaluation.optimisation.ntbea;

import core.AbstractPlayer;
import core.Game;
import evaluation.optimisation.ITPSearchSpace;
import evaluation.optimisation.NTBEAParameters;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static evaluation.optimisation.NTBEAParameters.Mode.StableNTBEA;

public class NTBEAEvaluator implements SolutionEvaluator {

    Random rnd = new Random();
    final ITPSearchSpace<NTBEAParameters> searchSpace;
    final NTBEAFunction fun;
    boolean debug = false;

    public NTBEAEvaluator(NTBEAFunction fun, ITPSearchSpace<NTBEAParameters>  searchSpace) {
        this.fun = fun;
        this.searchSpace = searchSpace;
    }

    @Override
    public void reset() {

    }

    @Override
    /**
     * This runs a complete NTBEA tuning run, and reports on the
     * final best settings found.
     *
     * Note that we have two search spaces. The one we are searching over (of NTBEA
     * parameters), and the one that NTBEA is using (which will likely
     * always be for function evaluation for time reasons).
     */
    public double evaluate(int[] settings) {
        // TODO: Run NTBEA to get a final best setting
        if (debug)
            System.out.printf("Starting evaluation of %s at %tT%n",
                    Arrays.toString(settings), System.currentTimeMillis());



        // TODO: Then find the actual value of the function at this setting
        // we now convert settings into the relevant values to feed to the function
//        double[] settings = new double[fun.dimension()];
//        for (int i = 0; i < settings.length; i++) {
//            settings[i] = (double) searchSpace.value(i, input[i]);
//        }
//        return fun.functionValue(settings);
        return 0;
    }

    @Override
    public SearchSpace searchSpace() {
        return searchSpace;
    }

    @Override
    public int nEvals() {
        return 0;
    }
}

