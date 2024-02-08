package evaluation.optimisation;

import evodef.SearchSpace;
import games.GameType;
import ntbea.MultiNTupleBanditEA;
import ntbea.NTupleSystem;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toMap;

public class MultiNTBEA extends NTBEA {

    GameMultiPlayerEvaluator multiPlayerEvaluator;

    public MultiNTBEA(NTBEAParameters parameters, GameType game, int nPlayers) {
        super(parameters, game, nPlayers);
        params.evalGames = 0;  // these are not used in the multi-player case (yet)

        searchFramework = new MultiNTupleBanditEA(landscapeModel, params.kExplore, params.neighbourhoodSize, nPlayers);

        // Initialise the GameEvaluator that will do all the heavy lifting
        multiPlayerEvaluator = new GameMultiPlayerEvaluator(
                game,
                params.searchSpace,
                nPlayers,
                stateHeuristic,
                params.seed
        );
    }

    @Override
    protected void runIteration() {
        super.runIteration();
        printDiversityResults(landscapeModel, params.kExplore);
    }

    @Override
    protected void runTrials() {
        multiPlayerEvaluator.reset();
        searchFramework.runTrial(multiPlayerEvaluator, params.iterationsPerRun);
    }


    private static List<int[]> generate(List<int[]> previous, int cardinality) {
        List<int[]> retValue = new ArrayList<>();
        for (int[] x : previous) {
            for (int i = 0; i < cardinality; i++) {
                int[] newX = new int[x.length + 1];
                for (int j = 0; j < x.length; j++) {
                    newX[j] = x[j];
                }
                newX[x.length] = i;
                retValue.add(newX);
            }
        }
        return retValue;
    }

    private static void printDiversityResults(NTupleSystem model, double kExplore) {
        // the idea is to run through all the points in the model, and initially order them by estimated value

        // first we need to generate all the possible int[] parameter settings
        // then getMeanEstimate() for each
        // order by descending value

        // pick a K, calculate the diverse set of points, and report this.
        SearchSpace ss = model.getSearchSpace();
        List<int[]> allTuples = new ArrayList<>();

        // For very large search spaces, we use the sampled points to reduce risks of memory problems with very large arrays
        int searchSpaceSize = IntStream.range(0, ss.nDims()).reduce(1, (acc, i) -> acc * ss.nValues(i));
        Set<int[]> allSampledPoints = model.getSampledPoints();
        if (searchSpaceSize < allSampledPoints.size()) {
            allTuples.add(new int[0]);
            for (int d = 0; d < ss.nDims(); d++) {
                allTuples = generate(allTuples, ss.nValues(d));
            }
        } else {
            allTuples = new ArrayList<>(allSampledPoints);
        }
        Map<int[], Double> tuplesWithValue = allTuples.stream().collect(toMap(t -> t, model::getMeanEstimate));
        double[] bestD = model.getBestOfSampled();
        int[] best = new int[bestD.length];
        for (int i = 0; i < bestD.length; i++)
            best[i] = (int) (bestD[i] + 0.5);
        double bestValue = model.getMeanEstimate(best);

        Set<int[]> bestSet = new HashSet<>();
        int diverseSize = allTuples.size();
        int optimalSize = 9;
        for (double k : Arrays.asList(0.0001, 0.0003, 0.001, 0.003, 0.01, 0.03, 0.1, 0.3)) {
            double modK = k * kExplore;
            Set<int[]> diverseGood = new HashSet<>();
            diverseGood.add(best);
            for (int[] tuple : tuplesWithValue.keySet()) {
                double value = tuplesWithValue.get(tuple);
                int distanceToNearest = diverseGood.stream().mapToInt(g -> manhattan(g, tuple)).min().orElse(0);
                if (value + modK * distanceToNearest > bestValue) {
                    // first we remove any from the set that are superseded by the new point
                    diverseGood.removeIf(t -> {
                        double v = model.getMeanEstimate(t);
                        int d = manhattan(tuple, t);
                        return v + modK * d < value;
                    });
                    diverseGood.add(tuple);
                }
            }
            System.out.printf("k = %.6f gives %d tuples out of %d%n", modK, diverseGood.size(), allTuples.size());
            // We
            if (Math.abs(Math.sqrt(optimalSize) - Math.sqrt(diverseGood.size())) < Math.abs(Math.sqrt(optimalSize) - Math.sqrt(diverseSize))) {
                diverseSize = diverseGood.size();
                bestSet = diverseGood;
            }
            // we can stop once we have at least the optimal number (to avoid thrashing compute)
            if (diverseGood.size() >= optimalSize)
                break;
        }
        System.out.println("\nBest settings with diversity:");
        for (int[] settings : bestSet) {
            System.out.printf("\t%.3f\t%s%n", model.getMeanEstimate(settings), Arrays.toString(settings));
        }

    }

    private static int manhattan(int[] x, int[] y) {
        int retValue = 0;
        for (int i = 0; i < x.length; i++) {
            retValue += Math.abs(x[i] - y[i]);
        }
        return retValue;
    }

}
