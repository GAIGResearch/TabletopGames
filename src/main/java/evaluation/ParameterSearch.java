package evaluation;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.CoreConstants;
import core.interfaces.IGameHeuristic;
import core.interfaces.IStateHeuristic;
import core.interfaces.IStatisticLogger;
import core.interfaces.ITunableParameters;
import evaluation.tournaments.RandomRRTournament;
import evodef.EvoAlg;
import evodef.SearchSpace;
import evodef.SolutionEvaluator;
import games.GameType;
import ntbea.MultiNTupleBanditEA;
import ntbea.NTupleBanditEA;
import ntbea.NTupleSystem;
import org.json.simple.JSONObject;
import players.PlayerFactory;
import utilities.Pair;
import utilities.StatSummary;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static evaluation.tournaments.AbstractTournament.TournamentMode.NO_SELF_PLAY;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
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
                        "\ttournament=    The number of games to run as a tournament with the winners of all NTBEA iterations (default is 0) \n" +
                        "\topponent=      The agent used as opponent. Default is not to use a specific opponent, but use MultiNTBEA. \n" +
                        "\t               This can any of: \n" +
                        "\t               \t'coop' uses the agent being tuned (via searchSpace) for all agents (i.e. for coop games)\n" +
                        "\t               \ta json-format file detailing the parameters, or\n" +
                        "\t               \tone of coop|mcts|rmhc|random|osla|<className>, or\n" +
                        "\t               \ta directory that contains one or more json-format files from which opponents will be sampled.\n" +
                        "\t               If className is specified, this must be the full name of a class implementing AbstractPlayer\n" +
                        "\t               with a no-argument constructor.\n" +
                        "\t               If tuneGame is set, then the opponent argument must be provided, and will be used for all players.\n" +
                        "\tplayerDupes=   If false (the default), and opponent specifies multiple files in a directory, then no agent will\n" +
                        "\t               be used twice in a single game (if there are enough agents to sample from). Set to true to allow dupes.\n" +
                        "\tgameParam=     The json-format file of game parameters to use. Defaults to standard rules and options.\n" +
                        "\ttuneGame       If supplied, then we will tune the game instead of tuning the agent.\n" +
                        "\t               In this case the searchSpace file must be relevant for the game.\n" +
                        "\teval=          Score|Ordinal|Heuristic|Win specifies what we are optimising (if not tuneGame). Defaults to Win.\n" +
                        "\t               If tuneGame, then instead the name of a IGameHeuristic class in the evaluation.heuristics package\n" +
                        "\t               must be provided, or the a json-format file that provides the requisite details. \n" +
                        "\t               The json-format file is needed if non-default settings for the IGameHeuristic are used.\n" +
                        "\tuseThreeTuples If specified then we use 3-tuples as well as 1-, 2- and N-tuples \n" +
                        "\tkExplore=      The k to use in NTBEA - defaults to 1.0 - this makes sense for win/lose games with a score in {0, 1}\n" +
                        "\t               For scores with larger ranges, we recommend scaling kExplore appropriately.\n" +
                        "\thood=          The size of neighbourhood to look at in NTBEA. Default is min(50, |searchSpace|/100) \n" +
                        "\trepeat=        The number of times NTBEA should be re-run, to find a single best recommendation \n" +
                        "\tverbose        Will log the results marginalised to each dimension, and the Top 10 best tuples for each run \n" +
                        "\tseed=          Random seed for Game use (not used by NTBEA itself). Defaults to System.currentTimeMillis()\n" +
                        "\tlogFile=       Output file with results of each run for easier statistical analysis\n"
        );

        if (argsList.size() < 3)
            throw new AssertionError("Must specify at least three parameters: searchSpace/ITunableParameters, NTBEA iterations, game");

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

        boolean useThreeTuples = Arrays.asList(args).contains("useThreeTuples");

        System.out.printf("Search space consists of %d states and %d possible 2-Tuples%s%n",
                searchSpaceSize, twoTupleSize, useThreeTuples ? String.format(" and %d 3-Tuples", threeTupleSize) : "");

        for (int i = 0; i < searchSpace.nDims(); i++) {
            int finalI = i;
            String allValues = IntStream.range(0, searchSpace.nValues(i))
                    .mapToObj(j -> searchSpace.value(finalI, j))
                    .map(Object::toString)
                    .collect(joining(", "));
            System.out.printf("%30s has %d values %s%n", searchSpace.name(i), searchSpace.nValues(i), allValues);
        }

        // Now initialise the other bits and pieces needed for the NTBEA package
        NTupleSystem landscapeModel = new NTupleSystem(searchSpace);
        landscapeModel.setUse3Tuple(useThreeTuples);
        landscapeModel.addTuples();

        boolean tuningGame = Arrays.asList(args).contains("tuneGame");

        if (getArg(args, "opponent", "").isEmpty()) {
            if (tuningGame)
                throw new AssertionError("If 'tuneGame' is set, then the opponent parameter is mandatory");
            runMultiNTBEA(landscapeModel, args);
        } else {
            runSingleNTBEA(landscapeModel, args);
        }
    }

    public static void runSingleNTBEA(NTupleSystem landscapeModel, String[] args) {

        boolean tuningGame = Arrays.asList(args).contains("tuneGame");
        int iterationsPerRun = Integer.parseInt(args[1]);
        GameType game = GameType.valueOf(args[2]);
        int repeats = getArg(args, "repeat", 1);
        int evalGames = getArg(args, "evalGames", iterationsPerRun / 5);
        double kExplore = getArg(args, "kExplore", 1.0);
        String opponentDescriptor = getArg(args, "opponent", "");
        boolean allowDupes = getArg(args, "playerDupes", false);
        boolean verbose = Arrays.asList(args).contains("verbose");
        int nPlayers = getArg(args, "nPlayers", game.getMinPlayers());
        long seed = getArg(args, "seed", System.currentTimeMillis());
        String logfile = getArg(args, "logFile", "");
        boolean statsLog = getArg(args, "statsLog", false);
        String evalMethod = getArg(args, "eval", "Win");
        String paramFile = getArg(args, "gameParam", "");
        AbstractParameters gameParams = AbstractParameters.createFromFile(game, paramFile);
        int tournamentGames = getArg(args, "tournament", 0);

        ITPSearchSpace searchSpace = (ITPSearchSpace) landscapeModel.getSearchSpace();
        int searchSpaceSize = IntStream.range(0, searchSpace.nDims()).reduce(1, (acc, i) -> acc * searchSpace.nValues(i));
        int hood = getArg(args, "hood", Math.min(50, searchSpaceSize / 100));

        NTupleBanditEA searchFramework = new NTupleBanditEA(landscapeModel, kExplore, hood);

        // Set up opponents
        List<AbstractPlayer> opponents = new ArrayList<>();
        // if we are in coop mode, then we have no opponents. This is indicated by leaving the list empty.
        if (!opponentDescriptor.equals("coop")) {
            // first check to see if we have a directory or not
            opponents = PlayerFactory.createPlayers(opponentDescriptor);
        }

        IGameHeuristic gameHeuristic = null;
        IStateHeuristic stateHeuristic = null;
        if (tuningGame) {
            if (new File(evalMethod).exists()) {
                // load from file
                gameHeuristic = Utils.loadClassFromFile(evalMethod);
            } else {
                if (evalMethod.contains(".json"))
                    throw new AssertionError("File not found : " + evalMethod);
                try {
                    Class<?> evalClass = Class.forName("evaluation.heuristics." + evalMethod);
                    gameHeuristic = (IGameHeuristic) evalClass.getConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("evaluation.heuristics." + evalMethod + " not found");
                } catch (NoSuchMethodException e) {
                    throw new AssertionError("evaluation.heuristics." + evalMethod + " has no no-arg constructor");
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                    throw new AssertionError("evaluation.heuristics." + evalMethod + " reflection error");
                }
            }

        } else {
            if (evalMethod.equals("Win"))
                stateHeuristic = (s, p) -> s.getPlayerResults()[p] == CoreConstants.GameResult.WIN_GAME ? 1.0 : 0.0;
            if (evalMethod.equals("Score"))
                stateHeuristic = AbstractGameState::getGameScore;
            if (evalMethod.equals("Heuristic"))
                stateHeuristic = AbstractGameState::getHeuristicScore;
            if (evalMethod.equals("Ordinal")) // we maximise, so the lowest ordinal position of 1 is best
                stateHeuristic = (s, p) -> -(double) s.getOrdinalPosition(p);
            if (stateHeuristic == null)
                throw new AssertionError("Invalid evaluation method provided: " + evalMethod);
        }
        // Initialise the GameEvaluator that will do all the heavy lifting
        GameEvaluator evaluator = new GameEvaluator(
                game,
                searchSpace,
                gameParams,
                nPlayers,
                opponents,
                seed,
                stateHeuristic,
                gameHeuristic,
                !allowDupes
        );

        // Get the results. And then log them.
        // This loops once for each complete repetition of NTBEA specified.
        // runNTBEA runs a complete set of trials, and spits out the mean and std error on the mean of the best sampled result
        // These mean statistics are calculated from the evaluation trials that are run after NTBEA is complete. (evalGames)
        Pair<Pair<Double, Double>, int[]> bestResult = new Pair<>(new Pair<>(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new int[0]);
        List<Object> winnersPerRun = new ArrayList<>();
        List<int[]> winnerSettings = new ArrayList<>();
        for (int mainLoop = 0; mainLoop < repeats; mainLoop++) {
            landscapeModel.reset();
            if (statsLog && !logfile.isEmpty())
                evaluator.statsLogger = IStatisticLogger.createLogger("evaluation.loggers.SummaryLogger", "Agent_" + String.format("%2d", repeats + 1) + "_" + logfile);
            Pair<Double, Double> r = runNTBEA(evaluator, null, searchFramework, iterationsPerRun, iterationsPerRun, evalGames, verbose);
            int[] winner = Arrays.stream(landscapeModel.getBestOfSampled())
                    .mapToInt(d -> (int) d)
                    .toArray();
            winnersPerRun.add(searchSpace.getAgent(winner));
            winnerSettings.add(winner);
            Pair<Pair<Double, Double>, int[]> retValue = new Pair<>(r, winner);
            printDetailsOfRun(retValue, searchSpace, logfile, verbose, evaluator.statsLogger);
            if (retValue.a.a > bestResult.a.a)
                bestResult = retValue;

        }
        if (tournamentGames > 0 && winnersPerRun.get(0) instanceof AbstractPlayer) {
            List<AbstractPlayer> players = winnersPerRun.stream().map(p -> (AbstractPlayer) p).collect(Collectors.toList());
            for (int i = 0; i < players.size(); i++) {
                players.get(i).setName(Arrays.toString(winnerSettings.get(i)));
            }
            RandomRRTournament tournament = new RandomRRTournament(players, game, nPlayers, NO_SELF_PLAY, tournamentGames, 0, seed, gameParams);
            tournament.verbose = false;
            tournament.runTournament();
            // create a new list of results in descending order of score
            IntToDoubleFunction cmp = evalMethod.equals("Ordinal") ? i -> -tournament.getOrdinalRank(i) : tournament::getWinRate;
            List<Integer> agentsInOrder = IntStream.range(0, players.size())
                    .boxed()
                    .sorted(Comparator.comparingDouble(cmp::applyAsDouble))
                    .collect(Collectors.toList());
            Collections.reverse(agentsInOrder);
            for (int index : agentsInOrder) {
                System.out.printf("Player %d %s\tWin Rate: %.3f +/- %.3f\tMean Ordinal: %.2f +/- %.2f%n", index, Arrays.toString(winnerSettings.get(index)),
                        tournament.getWinRate(index), tournament.getWinStdErr(index),
                        tournament.getOrdinalRank(index), tournament.getOrdinalStdErr(index));
                Pair<Double, Double> resultToReport = new Pair<>(tournament.getWinRate(index), tournament.getWinStdErr(index));
                if (evalMethod.equals("Ordinal"))
                    resultToReport = new Pair<>(tournament.getOrdinalRank(index), tournament.getOrdinalStdErr(index));
                logSummary(new Pair<>(resultToReport, winnerSettings.get(index)), searchSpace, "RRT_" + logfile);
            }
            bestResult = evalMethod.equals("Ordinal") ?
                    new Pair<>(new Pair<>(tournament.getOrdinalRank(agentsInOrder.get(0)), tournament.getOrdinalStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0))) :
                    new Pair<>(new Pair<>(tournament.getWinRate(agentsInOrder.get(0)), tournament.getWinStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0)));
        }
        System.out.println("\nFinal Recommendation: ");
        // we don't log the final run to file to avoid duplication
        printDetailsOfRun(bestResult, searchSpace, "", false, null);
    }

    public static void runMultiNTBEA(NTupleSystem landscapeModel, String[] args) {

        int iterationsPerRun = Integer.parseInt(args[1]);
        GameType game = GameType.valueOf(args[2]);
        int repeats = getArg(args, "repeat", 1);
        int evalGames = getArg(args, "evalGames", iterationsPerRun / 5);
        double kExplore = getArg(args, "kExplore", 1.0);
        boolean verbose = Arrays.asList(args).contains("verbose");
        int nPlayers = getArg(args, "nPlayers", game.getMinPlayers());
        long seed = getArg(args, "seed", System.currentTimeMillis());
        String logfile = getArg(args, "logFile", "");

        String evalMethod = getArg(args, "eval", "Win");
        IStateHeuristic stateHeuristic = null;
        if (evalMethod.equals("Win"))
            stateHeuristic = (s, p) -> s.getPlayerResults()[p] == CoreConstants.GameResult.WIN_GAME ? 1.0 : 0.0;
        if (evalMethod.equals("Score"))
            stateHeuristic = AbstractGameState::getGameScore;
        if (evalMethod.equals("Heuristic"))
            stateHeuristic = AbstractGameState::getHeuristicScore;
        if (evalMethod.equals("Ordinal")) // we maximise, so the lowest ordinal position of 1 is best
            stateHeuristic = (state, playerId) -> -(double) state.getOrdinalPosition(playerId);
        if (stateHeuristic == null)
            throw new AssertionError("Invalid evaluation method provided: " + evalMethod);

        ITPSearchSpace searchSpace = (ITPSearchSpace) landscapeModel.getSearchSpace();
        int searchSpaceSize = IntStream.range(0, searchSpace.nDims()).reduce(1, (acc, i) -> acc * searchSpace.nValues(i));
        int hood = getArg(args, "hood", Math.min(50, searchSpaceSize / 100));

        MultiNTupleBanditEA searchFramework = new MultiNTupleBanditEA(landscapeModel, kExplore, hood, nPlayers);

        // Initialise the GameEvaluator that will do all the heavy lifting
        GameMultiPlayerEvaluator evaluator = new GameMultiPlayerEvaluator(
                game,
                searchSpace,
                nPlayers,
                stateHeuristic,
                seed
        );

        // Get the results. And then log them.
        // This loops once for each complete repetition of NTBEA specified.
        // runNTBEA runs a complete set of trials, and spits out the mean and std error on the mean of the best sampled result
        // These mean statistics are calculated from the evaluation trials that are run after NTBEA is complete. (evalGames)
        Pair<Pair<Double, Double>, int[]> bestResult = new Pair<>(new Pair<>(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new int[0]);
        for (int mainLoop = 0; mainLoop < repeats; mainLoop++) {
            landscapeModel.reset();
            Pair<Double, Double> r = runNTBEA(null, evaluator, searchFramework, iterationsPerRun, iterationsPerRun, evalGames, verbose);
            Pair<Pair<Double, Double>, int[]> retValue = new Pair<>(r, Arrays.stream(landscapeModel.getBestOfSampled()).mapToInt(d -> (int) d).toArray());
            printDetailsOfRun(retValue, searchSpace, logfile, verbose, null);
            printDiversityResults(landscapeModel, kExplore);

            if (retValue.a.a > bestResult.a.a)
                bestResult = retValue;

        }
        System.out.println("\nFinal Recommendation: ");
        // we don't log the final run to file to avoid duplication
        printDetailsOfRun(bestResult, searchSpace, "", false, null);
    }


    /**
     * This just prints out some useful info on the NTBEA results. It lists the full underlying recommended
     * parameter settings, and the estimated mean score of these (with std error).
     *
     * @param data        The results of the NTBEA trials.
     *                    The {@code Pair<Double, Double>} is the mean and std error on the mean for the final recommendation,
     *                    as calculated from the post-NTBEA evaluation trials.
     *                    The double[] is the best sampled settings from the main NTBEA trials (that are then evaluated to get
     *                    a more accurate estimate of their utility).
     * @param searchSpace The relevant searchSpace
     */
    public static void printDetailsOfRun(Pair<Pair<Double, Double>, int[]> data, ITPSearchSpace searchSpace, String logFile, boolean verbose, IStatisticLogger statsLogger) {
        System.out.printf("Recommended settings have score %.3g +/- %.3g:\t%s\n %s%n",
                data.a.a, data.a.b,
                Arrays.stream(data.b).mapToObj(it -> String.format("%d", it)).collect(joining(", ")),
                IntStream.range(0, data.b.length).mapToObj(i -> new Pair<>(i, data.b[i]))
                        .map(p -> String.format("\t%s:\t%s\n", searchSpace.name(p.a), valueToString(p.a, p.b, searchSpace)))
                        .collect(joining(" ")));

        if (verbose && statsLogger != null && logFile.isEmpty()) {
            System.out.println("Agent Statistics: ");
            System.out.println(statsLogger);
        }

        if (!logFile.isEmpty()) {
            logSummary(data, searchSpace, logFile);

            if (statsLogger != null) {
                statsLogger.record("estimated value", data.a.a);
                for (int index = 0; index < data.b.length; index++) {
                    String key = searchSpace.name(index);
                    String value = valueToString(index, data.b[index], searchSpace);
                    statsLogger.record(key, value);
                }
                statsLogger.processDataAndFinish();
            }
        }

    }

    private static void logSummary(Pair<Pair<Double, Double>, int[]> data, ITPSearchSpace searchSpace, String logFile) {
        try {
            File log = new File(logFile);
            boolean fileExists = log.exists();
            FileWriter writer = new FileWriter(log, true);
            // if logFile does not yet exist, write a header line first
            if (!fileExists) {
                List<String> headers = new ArrayList<>();
                headers.addAll(Arrays.asList("estimated value", "standard error"));
                headers.addAll(searchSpace.getSearchKeys());
                writer.write(String.join("\t", headers) + "\n");
            }
            // then write the output
            String firstPart = String.format("%.4g\t%.4g\t", data.a.a, data.a.b);
            String values = IntStream.range(0, data.b.length).mapToObj(i -> new Pair<>(i, data.b[i]))
                    .map(p -> valueToString(p.a, p.b, searchSpace))
                    .collect(joining("\t"));
            writer.write(firstPart + values + "\n");
            writer.flush();
            writer.close();


        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + " : Error accessing file " + logFile);
        }
    }

    private static String valueToString(int paramIndex, int valueIndex, ITPSearchSpace ss) {
        Object value = ss.value(paramIndex, valueIndex);
        String valueString = value.toString();
        if (value instanceof Integer) {
            valueString = String.format("%d", value);
        } else if (value instanceof Double) {
            valueString = String.format("%.3g", value);
        }
        return valueString;
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
     * @return This returns {@code Pair<Mean, Std Error on Mean>} as calculated from the evaluation games
     */
    public static Pair<Double, Double> runNTBEA(SolutionEvaluator evaluator,
                                                GameMultiPlayerEvaluator multiPlayerEvaluator,
                                                EvoAlg searchFramework,
                                                int totalRuns, int reportEvery,
                                                int evalGames, boolean logResults) {

        NTupleSystem landscapeModel = (NTupleSystem) searchFramework.getModel();
        ITPSearchSpace searchSpace = (ITPSearchSpace) landscapeModel.getSearchSpace();

        // If reportEvery == totalRuns, then this will just loop once
        // (Which is the usual default)
        for (int iter = 0; iter < totalRuns / reportEvery; iter++) {
            if (evaluator != null) {
                evaluator.reset();
                searchFramework.runTrial(evaluator, reportEvery);
            } else {
                multiPlayerEvaluator.reset();
                searchFramework.runTrial(multiPlayerEvaluator, reportEvery);
            }

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
                System.out.printf("Summary of 1-tuple statistics after %d samples:%n", landscapeModel.numberOfSamples());

                IntStream.range(0, searchSpace.nDims()) // assumes that the first N tuples are the 1-dimensional ones
                        .mapToObj(i -> new Pair<>(searchSpace.name(i), landscapeModel.getTuples().get(i)))
                        .forEach(nameTuplePair ->
                                nameTuplePair.b.ntMap.keySet().stream().sorted().forEach(k -> {
                                    StatSummary v = nameTuplePair.b.ntMap.get(k);
                                    System.out.printf("\t%20s\t%s\t%d trials\t mean %.3g +/- %.2g%n", nameTuplePair.a, k, v.n(), v.mean(), v.stdErr());
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
                                        System.out.printf("\t%s\t%d trials\t mean %.3g +/- %.2g\t(NTuple estimate: %.3g)%n",
                                                item.a, item.b.n(), item.b.mean(), item.b.stdErr(), landscapeModel.getMeanEstimate(item.a.v))
                                )
                        );
            }
        }
        int[] winnerSettings = Arrays.stream(landscapeModel.getBestOfSampled())
                .mapToInt(d -> (int) d)
                .toArray();
        // now run the evaluation games on the final recommendation
        if (evaluator instanceof GameEvaluator && evalGames > 0) {
            double[] results = IntStream.range(0, evalGames)
                    .mapToDouble(answer -> evaluator.evaluate(winnerSettings)).toArray();

            double avg = Arrays.stream(results).average().orElse(0.0);
            double stdErr = Math.sqrt(Arrays.stream(results)
                    .map(d -> Math.pow(d - avg, 2.0)).sum()) / (evalGames - 1.0);

            return new Pair<>(avg, stdErr);
        } else {
            return new Pair<>(landscapeModel.getMeanEstimate(landscapeModel.getBestOfSampled()), 0.0);
        }
    }

}
