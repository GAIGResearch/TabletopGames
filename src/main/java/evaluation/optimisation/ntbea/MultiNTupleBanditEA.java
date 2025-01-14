package evaluation.optimisation.ntbea;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sml on 09/01/2017.
 */

public class MultiNTupleBanditEA extends NTupleBanditEA {

    public int playerCount;
    MultiSolutionEvaluator evaluator;

    public MultiNTupleBanditEA(LandscapeModel model, double kExplore, int nNeighbours, int players) {
        super(model, kExplore, nNeighbours);
        playerCount = players;
    }

    double[] fitness(MultiSolutionEvaluator evaluator, List<int[]> sol) {
        double[] retValue = new double[playerCount];
        for (int i = 0; i < nSamples; i++) {
            double[] fitness = evaluator.evaluate(sol);
            if (fitness.length != playerCount)
                throw new AssertionError("Discrepancy in Player Count - expecting " + playerCount + " results from evaluation");
            for (int p = 0; p < playerCount; p++)
                retValue[p] += fitness[p];
        }
        for (int p = 0; p < playerCount; p++)
            retValue[p] /= nSamples;
        return retValue;
    }

    public void runTrials(MultiSolutionEvaluator evaluator, int nEvals) {
        this.evaluator = evaluator;
        // set  up some convenient reference
        SearchSpace searchSpace = landscapeModel.getSearchSpace();

        nNeighbours = (int) Math.min(nNeighbours, SearchSpaceUtil.size(searchSpace) / 4);
        if (nNeighbours < 5) nNeighbours = 5;
        //     System.out.println("Set neighbours to: " + nNeighbours);

        // then each time around the loop try the following
        // create a neighbourhood set of points and pick the best one that combines it's exploitation and evaluation scores

        List<int[]> p = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++)
            p.add(SearchSpaceUtil.randomPoint(searchSpace));

        for (int i = 0; i < nEvals; i++) {
            // each time around the loop we make one fitness evaluation of p
            // and add this NEW information to the memory
            //int prevEvals = evaluator.nEvals();

            // the new version enables resampling
            double[] fitness;
            if (nSamples == 1) {
                fitness = evaluator.evaluate(p);
            } else {
                fitness = fitness(evaluator, p);
            }

            // register all of the evaluated settings with the model
            for (int j = 0; j < playerCount; j++)
                landscapeModel.addPoint(p.get(j), fitness[j]);


            // and then explore the neighbourhood around p, balancing exploration and exploitation
            // we currently hardcode one mutation function to randomly change one setting at a time

            // we search independently for each player based on their current p[]

            int nDims = searchSpace.nDims();
            double bestSoFar = Double.NEGATIVE_INFINITY;
            List<int[]> newP = new ArrayList<>();
            for (int player = 0; player < playerCount; player++) {
                int[] settingToTryNext = new int[0];
                for (int n = 0; i < nNeighbours; i++) {
                    int[] pp = Arrays.copyOf(p.get(player), p.get(player).length);
                    boolean mutation = false;
                    for (int d = 0; d < nDims; d++) {
                        if (rnd.nextDouble() < 1.0 / nDims) {
                            pp[d] = rnd.nextInt(searchSpace.nValues(d) - 1);
                            if (p.get(player)[d] <= pp[d]) pp[d]++;
                            mutation = true;
                        }
                    }
                    // if no mutations made, then change one
                    if (!mutation) {
                        int d = rnd.nextInt(searchSpace.nDims());
                        pp[d] = rnd.nextInt(searchSpace.nValues(d) - 1);
                        if (p.get(player)[d] <= pp[d]) pp[d]++;
                    }

                    double estimatedUpperBound = landscapeModel.getUpperBound(pp);
                    if (estimatedUpperBound > bestSoFar)
                        settingToTryNext = pp;
                }
                newP.add(settingToTryNext);
            }
            p = newP;
        }
    }

}
