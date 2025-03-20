package evaluation.optimisation.ntbea;

import evaluation.optimisation.ITPSearchSpace;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.optimisation.ntbea.functions.FunctionSearchSpace;
import evaluation.optimisation.ntbea.functions.NTBEAFunction;
import org.json.simple.JSONObject;
import utilities.JSONUtils;
import utilities.Pair;

import java.util.Arrays;

public class NTBEAEvaluator implements SolutionEvaluator {

    final ITPSearchSpace<NTBEAParameters> searchSpace;
    final NTBEAFunction fun;
    final int discretisationLevel;
    final NTBEAParameters parameters;
    boolean debug = false;

    public NTBEAEvaluator(NTBEAFunction fun, int discretisationLevel, SearchSpace searchSpace, NTBEAParameters parameters) {
        this.fun = fun;
        this.searchSpace = (ITPSearchSpace<NTBEAParameters>) searchSpace;
        this.discretisationLevel = discretisationLevel;
        this.parameters = parameters;
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
        if (debug)
            System.out.printf("Starting evaluation of %s at %tT%n",
                    Arrays.toString(settings), System.currentTimeMillis());

        // what we can do here is run through the settings, and set up the NTBEAParameters that we want
        JSONObject json = searchSpace.constructAgentJSON(settings);
        NTBEAParameters params = JSONUtils.loadClassFromJSON(json);

        // We now set up iterations based on the budget (this is an awkward override of the usual meaning of 'budget')
        int iterationsPerRun = (params.tournamentGames - params.OSDBudget) / params.repeats / params.evaluationsPerTrial - params.evalGames;
        params.setParameterValue("iterations", iterationsPerRun);
        if (params.iterationsPerRun <= 0)
            throw new AssertionError("Budget too low for NTBEA");
        params.setParameterValue("matchups", 0);
        params.searchSpace = new FunctionSearchSpace(discretisationLevel, fun);
        params.logFile = "";
        NTBEA ntbea = new NTBEA(params, fun, discretisationLevel);

        Pair<Object, int[]> finalRecommendation = ntbea.run();

        // Then find the actual value of the function at this setting
        int[] functionSettings = finalRecommendation.b;
        double[] f = new double[fun.dimension()];
        for (int i = 0; i < functionSettings.length; i++) {
            f[i] = (double) params.searchSpace.value(i, functionSettings[i]);
        }
        return fun.functionValue(f);
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

