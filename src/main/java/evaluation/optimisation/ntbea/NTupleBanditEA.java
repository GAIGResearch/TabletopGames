package evaluation.optimisation.ntbea;

import evaluation.optimisation.NTBEAParameters;
import utilities.StatSummary;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by sml on 09/01/2017.
 */

public class NTupleBanditEA {

    // public NTupleSystem banditLandscapeModel;
    protected LandscapeModel landscapeModel;
    // the number of neighbours to explore around the current point each time
    // they are only explored IN THE FITNESS LANDSCAPE MODEL, not by sampling the fitness function
    int nNeighbours;
    int nSamples = 1;

    public NTupleBanditEA(LandscapeModel model, NTBEAParameters params) {
        landscapeModel = model;
        this.nNeighbours = params.neighbourhoodSize;
        this.nSamples = params.evaluationsPerTrial;
    }

    StatSummary fitness(SolutionEvaluator evaluator, int[] sol) {
        StatSummary ss = new StatSummary();
        for (int i = 0; i < nSamples; i++) {
            double fitness = evaluator.evaluate(sol);
            ss.add(fitness);
        }
        return ss;
    }

    Random rnd = new Random();
    SolutionEvaluator evaluator;

    public void runTrial(SolutionEvaluator evaluator, int nEvals) {
        this.evaluator = evaluator;
        // set  up some convenient reference
        SearchSpace searchSpace = landscapeModel.getSearchSpace();

        nNeighbours = (int) Math.min(nNeighbours, SearchSpaceUtil.size(searchSpace) / 4);
        if (nNeighbours < 5) nNeighbours = 5;

        // then each time around the loop try the following
        // create a neighbourhood set of points and pick the best one that combines its exploitation and evaluation scores

        int[] p = SearchSpaceUtil.randomPoint(searchSpace);

        for (int i = 0; i < nEvals; i++) {
            // each time around the loop we make one fitness evaluation of p
            // and add this NEW information to the memory
            double fitness;
            if (nSamples == 1) {
                fitness = evaluator.evaluate(p);
            } else {
                fitness = fitness(evaluator, p).mean();
            }

            landscapeModel.addPoint(p, fitness);

            // and then explore the neighbourhood around p, balancing exploration and exploitation
            // we currently hardcode one mutation function to randomly change one setting at a time

            int nDims = searchSpace.nDims();
            double bestSoFar = Double.NEGATIVE_INFINITY;
            int[] settingToTryNext = new int[0];
            for (int n = 0; n < nNeighbours; n++) {
                int[] pp = Arrays.copyOf(p, p.length);
                boolean mutation = false;
                for (int d = 0; d < nDims; d++) {
                    if (rnd.nextDouble() < 1.0 / nDims) {
                        pp[d] = rnd.nextInt(searchSpace.nValues(d) - 1);
                        if (p[d] <= pp[d]) pp[d]++;
                        mutation = true;
                    }
                }
                // if no mutations made, then change one
                if (!mutation) {
                    int d = rnd.nextInt(searchSpace.nDims());
                    pp[d] = rnd.nextInt(searchSpace.nValues(d) - 1);
                    if (p[d] <= pp[d]) pp[d]++;
                }

                double estimatedUpperBound = landscapeModel.getUpperBound(pp);
                if (estimatedUpperBound > bestSoFar) {
                    settingToTryNext = pp;
                    bestSoFar = estimatedUpperBound;
                }
            }

            if (settingToTryNext.length == 0)
                settingToTryNext = p;
            p = settingToTryNext;
        }
    }
}
