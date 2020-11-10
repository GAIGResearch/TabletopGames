package evaluation;

import core.AbstractPlayer;
import core.interfaces.ITunableParameters;
import evodef.*;
import games.GameType;
import ntbea.*;
import org.json.simple.JSONObject;
import players.PlayerFactory;
import players.simple.RandomPlayer;
import utilities.Pair;
import utilities.StatSummary;
import utilities.SummaryLogger;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utilities.Utils.getArg;
import static utilities.Utils.loadJSONFile;

public class ParameterSearch {

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.isEmpty() || argsList.contains("--help") || argsList.contains("-h")) System.out.println(
                "The first three arguments must be \n" +
                        "\t<filename for searchSpace definition> or <ITunableParameters classname>\n" +
                        "\t<number of NTBEA iterations>\n" +
                        "\t<game type> \n" +
                        "Then there are a number of optional arguments:\n" +
                        "\tnPlayers=      The total number of players in each game (the default is game.Min#players) \n " +
                        "\tevalGames=     The number of games to run with the best predicted setting to estimate its true value (default is 20% of NTBEA iterations) \n" +
                        "\topponent=      The agent used as opponent. Default is a Random player. \n" +
                        "\t               This can either be a json-format file detailing the parameters, or\n" +
                        "\t               one of mcts|rmhc|random|osla|<className>  \n" +
                        "\t               If className is specified, this must be the full name of a class implementing AbstractPlayer\n" +
                        "\t               with a no-argument constructor\n" +
                        "\tuseThreeTuples If specified then we use 3-tuples as well as 1-, 2- and N-tuples \n" +
                        "\tkExplore=      The k to use in NTBEA - defaults to 100.0 \n" +
                        "\thood=          The size of neighbourhood to look at in NTBEA. Default is min(50, |searchSpace|/100) \n" +
                        "\trepeat=        The number of times NTBEA should be re-run, to find a single best recommendation \n" +
                        "\tverbose        Will log the results marginalised to each dimension, and the Top 10 best tuples for each run \n" +
                        "\tseed=          Random seed for Game use (not used for NTBEA). Defaults to System.currentTimeMillis()"
        );

        if (argsList.size() < 3)
            throw new AssertionError("Must specify at least three parameters: searchSpace/ITunableParameters, NTBEA iterations, game");
        int iterationsPerRun = Integer.parseInt(args[1]);
        GameType game = GameType.valueOf(args[2]);
        int repeats = getArg(args, "repeat", 1);
        int evalGames = getArg(args, "evalGames", iterationsPerRun / 5);
        double kExplore = getArg(args, "kExplore", 100.0);
        String opponentDescriptor = getArg(args, "opponent", "");
        boolean verbose = Arrays.asList(args).contains("verbose");
        int nPlayers = getArg(args, "nPlayers", game.getMinPlayers());
        long seed = getArg(args, "seed", System.currentTimeMillis());

        // Create the SearchSpace, and report some useful stuff to the console.
        ITPSearchSpace searchSpace;
        boolean fileExists = (new File(args[0])).exists();
        try {
            String className = args[0];
            Constructor<ITunableParameters> constructor;
            JSONObject json = null;
            if (fileExists) {
                // We import the file as a JSONObject
                json = loadJSONFile(args[0]);
                className = (String) json.get("class");
                if (className == null)
                    throw new AssertionError("No class property found in JSON file. This is required to specify the ITunableParameters class that the file complements");
            }
            // we pull in the ITP referred to in the JSON file, or directly as args[0]
            Class<ITunableParameters> itpClass = (Class<ITunableParameters>) Class.forName(className);
            constructor = itpClass.getConstructor();
            ITunableParameters itp = constructor.newInstance();
            // We then initialise the ITPSearchSpace with this ITP and the JSON details
            searchSpace = fileExists ? new ITPSearchSpace(itp, json) : new ITPSearchSpace(itp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getClass() + " : " + e.getMessage() + "Error loading ITunableParameters class in " + args[0]);
        }

        int searchSpaceSize = IntStream.range(0, searchSpace.nDims()).reduce(1, (acc, i) -> acc * searchSpace.nValues(i));
        int twoTupleSize = IntStream.range(0, searchSpace.nDims() - 1)
                .map(i -> searchSpace.nValues(i) *
                        IntStream.range(i + 1, searchSpace.nDims())
                                .map(searchSpace::nValues).sum()
                ).sum();
        int threeTupleSize = IntStream.range(0, searchSpace.nDims() - 2)
                .map(i -> searchSpace.nValues(i) *
                        IntStream.range(i + 1, searchSpace.nDims()).map(j ->
                                searchSpace.nValues(j) * IntStream.range(j + 1, searchSpace.nDims())
                                        .map(searchSpace::nValues).sum()
                        ).sum()
                ).sum();

        int hood = getArg(args, "hood", Math.min(50, searchSpaceSize / 100));
        boolean useThreeTuples = Arrays.asList(args).contains("useThreeTuples");

        System.out.println(String.format("Search space consists of %d states and %d possible 2-Tuples%s",
                searchSpaceSize, twoTupleSize, useThreeTuples ? String.format(" and %d 3-Tuples", threeTupleSize) : ""));

        for (int i = 0; i < searchSpace.nDims(); i++) {
            int finalI = i;
            String allValues = IntStream.range(0, searchSpace.nValues(i))
                    .mapToObj(j -> searchSpace.value(finalI, j))
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            System.out.printf("%20s has %d values %s%n", searchSpace.name(i), searchSpace.nValues(i), allValues);
        }

        // Now initialise the other bits and pieces needed for the NTBEA package
        NTupleSystem landscapeModel = new NTupleSystem(searchSpace);
        landscapeModel.setUse3Tuple(useThreeTuples);
        landscapeModel.addTuples();
        NTupleBanditEA searchFramework = new NTupleBanditEA(landscapeModel, kExplore, hood);

        // Set up opponents
        List<AbstractPlayer> opponents = new ArrayList<>();
        for (int i = 0; i < nPlayers; i++) {
            AbstractPlayer opponent = opponentDescriptor.isEmpty() ? new RandomPlayer() : PlayerFactory.createPlayer(opponentDescriptor);
            opponents.add(opponent);
        }

        // TODO: At some later point we also need to allow different evaluation functions to be used. Win/Lose / Score / Ordinal position
        // and then for Game tuning other items that measure how close the result is, etc.

        // Initialise the GameEvaluator that will do all the heavy lifting
        GameEvaluator evaluator = new GameEvaluator(
                game,
                searchSpace,
                nPlayers,
                opponents,
                seed,
                true
        );

        // Get the results. And then print them out.
        // This loops once for each complete repetition of NTBEA specified.
        // runNTBEA runs a complete set of trials, and spits out the mean and std error on the mean of the best sampled result
        // These mean statistics are calculated from the evaluation trials that are run after NTBEA is complete. (evalGames)
        Pair<Pair<Double, Double>, double[]> bestResult = new Pair<>(new Pair<>(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new double[0]);
        for (int mainLoop = 0; mainLoop < repeats; mainLoop++) {
            landscapeModel.reset();
            Pair<Double, Double> r = runNTBEA(evaluator, searchFramework, iterationsPerRun, iterationsPerRun, evalGames, verbose);
            Pair<Pair<Double, Double>, double[]> retValue = new Pair<>(r, landscapeModel.getBestOfSampled());
            printDetailsOfRun(retValue, searchSpace);
            if (verbose) {
                System.out.println("MCTS Statistics: ");
                System.out.println(evaluator.statsLogger.toString());
            }
            evaluator.statsLogger = new SummaryLogger();
            if (retValue.a.a > bestResult.a.a)
                bestResult = retValue;

        }
        System.out.println("\nFinal Recommendation: ");
        printDetailsOfRun(bestResult, searchSpace);
    }


    /**
     * This just prints out some useful info on the NTBEA results. It lists the full underlying recommended
     * parameter settings, and the estimated mean score of these (with std error).
     *
     * @param data        The results of the NTBEA trials.
     *                    The Pair<Double, Double> is the mean and std error on the mean for the final recommendation,
     *                    as calculated from the post-NTBEA evaluation trials.
     *                    The double[] is the best sampled settings from the main NTBEA trials (that are then evaluated to get
     *                    a more accurate estimate of their utility).
     * @param searchSpace The relevant searchSpace
     */
    private static void printDetailsOfRun(Pair<Pair<Double, Double>, double[]> data, ITPSearchSpace searchSpace) {
        System.out.println(String.format("Recommended settings have score %.3g +/- %.3g:\t%s\n %s",
                data.a.a, data.a.b,
                Arrays.stream(data.b).mapToObj(it -> String.format("%.0f", it)).collect(Collectors.joining(", ")),
                IntStream.range(0, data.b.length).mapToObj(i -> new Pair<>(i, data.b[i]))
                        .map(p -> {
                                    int paramIndex = p.a;
                                    double valueIndex = p.b;
                                    Object value = searchSpace.value(paramIndex, (int) valueIndex);
                                    String valueString = value.toString();
                                    if (value instanceof Integer) {
                                        valueString = String.format("%d", value);
                                    } else if (value instanceof Double) {
                                        valueString = String.format("%.3g", value);
                                    }
                                    return String.format("\t%s:\t%s\n", searchSpace.name(paramIndex), valueString);
                                }
                        ).collect(Collectors.joining(" "))));
    }


    /**
     * The workhorse.
     *
     * @param evaluator       The SolutionEvaluator that provides a sample score for a set of parameters
     * @param searchFramework The NTBEA search framework. This maintains the model of parameter space
     *                        and decides what settings to try next.
     * @param totalRuns       The total number of NTBEA trials.
     * @param reportEvery     This can be used to report interim progress (but only used if logResults=true)
     *                        Will report current NTBEA stats after this number of trials.
     * @param evalGames       The number of evaluation trials to run on the final NBEA recommendation.
     *                        This is to get a good estimate of the true value of the recommendation.
     * @param logResults      If true, then logs lots of data on the process. (Marginal statistics in each dimension
     *                        and the Top 10 Tuples by trials.) This can be useful to visualise the parameter
     *                        landscape beyond the simple final recommendation and get a feel for which dimensions
     *                        really matter.
     * @return This returns Pair<Mean, Std Error on Mean> as calculated from the evaluation games
     */
    public static Pair<Double, Double> runNTBEA(GameEvaluator evaluator,
                                                EvoAlg searchFramework,
                                                int totalRuns, int reportEvery,
                                                int evalGames, boolean logResults) {

        NTupleSystem landscapeModel = (NTupleSystem) searchFramework.getModel();
        SearchSpace searchSpace = landscapeModel.getSearchSpace();

        // If reportEvery == totalRuns, then this will just loop once
        // (Which is the usual default)
        for (int iter = 0; iter < totalRuns / reportEvery; iter++) {
            evaluator.reset();
            searchFramework.runTrial(evaluator, reportEvery);

            if (logResults) {
                System.out.println("Current best sampled point (using mean estimate): " +
                        Arrays.toString(landscapeModel.getBestOfSampled()) +
                        String.format(", %.3g", landscapeModel.getMeanEstimate(landscapeModel.getBestOfSampled())));

                String tuplesExploredBySize = Arrays.toString(IntStream.rangeClosed(1, searchSpace.nDims())
                        .map(size -> landscapeModel.getTuples().stream()
                                .filter(t -> t.tuple.length == size)
                                .mapToInt(it -> it.ntMap.size())
                                .sum()
                        ).toArray());

                System.out.println("Tuples explored by size: " + tuplesExploredBySize);
                System.out.println(String.format("Summary of 1-tuple statistics after %d samples:", landscapeModel.numberOfSamples()));

                IntStream.range(0, searchSpace.nDims()) // assumes that the first N tuples are the 1-dimensional ones
                        .mapToObj(i -> new Pair<>(searchSpace.name(i), landscapeModel.getTuples().get(i)))
                        .forEach(nameTuplePair ->
                                nameTuplePair.b.ntMap.keySet().stream().sorted().forEach(k -> {
                                    StatSummary v = nameTuplePair.b.ntMap.get(k);
                                    System.out.println(String.format("\t%20s\t%s\t%d trials\t mean %.3g +/- %.2g", nameTuplePair.a, k, v.n(), v.mean(), v.stdErr()));
                                })
                        );

                System.out.println("\nSummary of 10 most tried full-tuple statistics:");
                landscapeModel.getTuples().stream()
                        .filter(t -> t.tuple.length == searchSpace.nDims())
                        .forEach(t -> t.ntMap.keySet().stream()
                                .map(k -> new Pair<>(k, t.ntMap.get(k)))
                                .sorted(Comparator.comparing(p -> -p.b.n()))
                                .limit(10)
                                .forEach(item ->
                                        System.out.println(String.format("\t%s\t%d trials\t mean %.3g +/- %.2g\t(NTuple estimate: %.3g)",
                                                item.a, item.b.n(), item.b.mean(), item.b.stdErr(), landscapeModel.getMeanEstimate(item.a.v)))
                                )
                        );
            }
        }
        // now run the evaluation games on the final recommendation
        if (evalGames > 0) {
            evaluator.reportStatistics = true;
            double[] results = IntStream.range(0, evalGames)
                    .mapToDouble(answer -> {
                        int[] settings = Arrays.stream(landscapeModel.getBestOfSampled())
                                .mapToInt(d -> (int) d)
                                .toArray();
                        return evaluator.evaluate(settings);
                    }).toArray();

            double avg = Arrays.stream(results).average().getAsDouble();
            double stdErr = Math.sqrt(Arrays.stream(results)
                    .map(d -> Math.pow(d - avg, 2.0)).sum()) / (evalGames - 1.0);

            evaluator.reportStatistics = false;
            return new Pair<>(avg, stdErr);
        } else {
            return new Pair<>(landscapeModel.getMeanEstimate(landscapeModel.getBestOfSampled()), 0.0);
        }
    }

}
