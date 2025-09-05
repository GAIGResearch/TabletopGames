package evaluation.optimisation;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.interfaces.IGameHeuristic;
import core.interfaces.IStateHeuristic;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import evaluation.optimisation.ntbea.functions.FunctionEvaluator;
import evaluation.optimisation.ntbea.functions.NTBEAFunction;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import evaluation.optimisation.ntbea.*;
import players.PlayerFactory;
import players.heuristics.OrdinalPosition;
import players.heuristics.PureScoreHeuristic;
import players.heuristics.WinOnlyHeuristic;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static evaluation.RunArg.*;
import static java.util.stream.Collectors.joining;

public class NTBEA {

    NTBEAParameters params;
    NTupleSystem landscapeModel;
    NTupleBanditEA searchFramework;
    List<Object> winnersPerRun = new ArrayList<>();
    List<int[]> winnerSettings = new ArrayList<>();
    List<int[]> elites = new ArrayList<>();
    Pair<Pair<Double, Double>, int[]> bestResult = new Pair<>(new Pair<>(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new int[0]);
    SolutionEvaluator evaluator;
    GameType game;
    NTBEAFunction function;
    int nPlayers;
    int discretisationLevel;
    int currentIteration = 0;
    IStateHeuristic stateHeuristic;
    IGameHeuristic gameHeuristic;

    protected NTBEA(NTBEAParameters parameters) {
        this.params = parameters;
        landscapeModel = new NTupleSystem(params);
        searchFramework = new NTupleBanditEA(landscapeModel, params);
    }

    public NTBEA(NTBEAParameters parameters, NTBEAFunction function, int discretisationLevel) {
        this(parameters);
        this.function = function;
        this.discretisationLevel = discretisationLevel;

        evaluator = new FunctionEvaluator(function, landscapeModel.getSearchSpace());
    }

    public NTBEA(NTBEAParameters parameters, GameType game, int nPlayers) {
        // Now initialise the other bits and pieces needed for the NTBEA package
        this(parameters);
        this.game = game;
        this.nPlayers = nPlayers;
        // Set up opponents
        // if we are in coop mode, then we have no opponents. This is indicated by leaving the list empty.
        List<AbstractPlayer> opponents = params.mode == NTBEAParameters.Mode.CoopNTBEA ? new ArrayList<>()
                : PlayerFactory.createPlayers(params.opponentDescriptor);

        if (params.tuningGame) {
            gameHeuristic = JSONUtils.loadClass(params.evalMethod);
        } else {
            // Add check that the number of final matchups is reasonable given the number of repeats
            if (params.tournamentGames > 0) {
                int matchupsPerGame = Utils.gamesPerMatchup(nPlayers, params.repeats, params.tournamentGames, false);
                if (matchupsPerGame < 1) {
                    throw new IllegalArgumentException(String.format("Higher budget needed for the final tournament. There will be %d permutations of agents to positions in exhaustive mode, which is more than %d game in the available budget.",
                            Utils.playerPermutations(nPlayers, params.repeats, false), params.tournamentGames));
                }
            }

            if (params.evalMethod.equals("Win"))
                stateHeuristic = new WinOnlyHeuristic();
            if (params.evalMethod.equals("Score"))
                stateHeuristic = new PureScoreHeuristic();
            if (params.evalMethod.equals("Heuristic"))
                stateHeuristic = AbstractGameState::getHeuristicScore;
            if (params.evalMethod.equals("Ordinal")) // we maximise, so the lowest ordinal position of 1 is best
                stateHeuristic = new OrdinalPosition();
            if (stateHeuristic == null)
                throw new AssertionError("Invalid evaluation method provided: " + params.evalMethod);
        }
        // Initialise the GameEvaluator that will do all the heavy lifting
        evaluator = new GameEvaluator(
                game,
                params,
                nPlayers,
                opponents,
                stateHeuristic,
                gameHeuristic,
                true);
    }

    public void setOpponents(List<AbstractPlayer> opponents) {
        if (evaluator instanceof GameEvaluator gameEvaluator) {
            gameEvaluator.opponents = opponents;
        } else {
            throw new AssertionError("Cannot set opponents for a non-game evaluator");
        }
    }

    public void addElite(int[] settings) {
        // We use these settings as players in our final tournament
        elites.add(settings);
    }

    // Only works if the search space is an instance of ITPSearchSpace
    // This sets a tunable parameter that is *NOT* in the search space itself to the specified value
    // This will ensure that all agents in the run use this parameter
    public void fixTunableParameter(String key, Object value) {
        for (int i = 0; i < params.searchSpace.nDims(); i++) {
            if (params.searchSpace.name(i).equals(key)) {
                throw new IllegalArgumentException("Cannot set a tunable parameter that is in the search space. Use fixSearchDimension instead.");
            }
        }
        if (params.searchSpace instanceof ITPSearchSpace<?> itp) {
            if (itp.itp instanceof TunableParameters<?> tp) {
                tp.setParameterValue(key, value);
                return;
            }
        }
        throw new IllegalArgumentException("Cannot add to search space - not an ITPSearchSpace");
    }

    /**
     * This returns the optimised object, plus the settings that produced it (indices to the values in the search space)
     *
     * @return
     */
    public Pair<Object, int[]> run() {

        for (currentIteration = 0; currentIteration < params.repeats; currentIteration++) {
            // Check for existence of the output file. If it already exists, then we
            // load the file, convert it to add to winnerSettings. Then skip this iteration
            String iterationFilename = params.destDir + File.separator + "Recommended_" + currentIteration + ".json";
            if ((new File(iterationFilename)).exists() && params.searchSpace instanceof ITPSearchSpace<?> itp) {
                int[] settings = itp.settingsFromJSON(iterationFilename);
                winnerSettings.add(settings);
                winnersPerRun.add(itp.instantiate(settings));
                System.out.println("NTBEA for iteration " + currentIteration + " has already completed - skipping");
            } else {
                runIteration();
                if (params.searchSpace instanceof ITPSearchSpace<?> itp) {
                    itp.writeAgentJSON(winnerSettings.get(winnerSettings.size() - 1), iterationFilename);
                }
            }
        }

        // After all runs are complete, if tournamentGames are specified, then we allow all the
        // winners from each iteration to play in a tournament and pick the winner of this tournament
        if (params.tournamentGames > 0 && winnersPerRun.get(0) instanceof AbstractPlayer) {
            if (!elites.isEmpty()) {
                // first of all we add the elites into winnerSettings, and winnersPerRun
                // i.e. we effectively add an extra 'run' for each elite
                for (int[] elite : elites) {
                    winnerSettings.add(elite);
                    winnersPerRun.add(((ITPSearchSpace<?>) params.searchSpace).instantiate(elite));
                }
            }

            List<AbstractPlayer> players = winnersPerRun.stream().map(p -> (AbstractPlayer) p).collect(Collectors.toList());
            for (int i = 0; i < players.size(); i++) {
                players.get(i).setName("Winner " + i + " : " + Arrays.toString(winnerSettings.get(i)));
            }
            // Given we have N players in each game, and a total of M agents (the number of NTBEA iterations), we
            // can reduce the variance in the results (and hence the accuracy of picking the best agent) by using the exhaustive mode
            // this does rely on not having, say 20 NTBEA iterations on a 6-player game (38k combinations); but assuming
            // the advice of 10 or fewer iterations holds, then even on a 5-player game we have 252 combinations, which is fine.
            int nTeams = params.byTeam ? game.createGameInstance(nPlayers, params.gameParams).getGameState().getNTeams() : nPlayers;
            if (players.size() < nTeams) {
                System.out.println("Not enough players to run a tournament with " + nTeams + " players. Skipping the final tournament - " +
                        "check the repeats options is at least equal to the number of players.");
            } else {
                Map<RunArg, Object> config = new HashMap<>();
                config.put(matchups, params.tournamentGames);
                if (players.size() < nPlayers) {
                    // if we don't have enough players to fill the game, then we will need to use self-play
                    config.put(RunArg.mode, "exhaustiveSP");
                } else {
                    config.put(RunArg.mode, "exhaustive");
                }
                config.put(byTeam, true);
                config.put(RunArg.distinctRandomSeeds, 0);
                config.put(RunArg.budget, params.budget);
                config.put(RunArg.verbose, false);
                config.put(RunArg.destDir, params.destDir);
                RoundRobinTournament tournament = new RoundRobinTournament(players, game, nPlayers, params.gameParams, config);
                createListeners().forEach(tournament::addListener);
                tournament.run();
                // create a new list of results in descending order of score
                IntToDoubleFunction cmp = params.evalMethod.equals("Ordinal") ? tournament::getOrdinalAlphaRank : tournament::getWinRateAlphaRank;
                List<Integer> agentsInOrder = IntStream.range(0, players.size())
                        .boxed()
                        .sorted(Comparator.comparingDouble(cmp::applyAsDouble))
                        .collect(Collectors.toList());
                Collections.reverse(agentsInOrder);
                params.logFile = "RRT_" + params.logFile;
                for (int index : agentsInOrder) {
                    if (params.verbose)
                        System.out.printf("Player %d %s\tWin Rate: %.3f +/- %.3f\tMean Ordinal: %.2f +/- %.2f\tWinAlpha: %.3f\tOrdinalAlpha: %.3f%n", index, Arrays.toString(winnerSettings.get(index)),
                                tournament.getWinRate(index), tournament.getWinStdErr(index),
                                tournament.getOrdinalRank(index), tournament.getOrdinalStdErr(index),
                                tournament.getWinRateAlphaRank(index), tournament.getOrdinalAlphaRank(index));
                    Pair<Double, Double> resultToReport = new Pair<>(tournament.getWinRate(index), tournament.getWinStdErr(index));
                    if (params.evalMethod.equals("Ordinal"))
                        resultToReport = new Pair<>(tournament.getOrdinalRank(index), tournament.getOrdinalStdErr(index));

                    logSummary(new Pair<>(resultToReport, winnerSettings.get(index)), params);
                }
                params.logFile = params.logFile.substring(4);
                // Note that the bestResult uses AlphaRank, as this factors in *who* the agent beats and
                // helps prevent a high win rate against only weak opponents from being the best result
                bestResult = params.evalMethod.equals("Ordinal") ?
                        new Pair<>(new Pair<>(tournament.getOrdinalRank(agentsInOrder.get(0)), tournament.getOrdinalStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0))) :
                        new Pair<>(new Pair<>(tournament.getWinRate(agentsInOrder.get(0)), tournament.getWinStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0)));

                // We then want to check the win rate against the elite agent (if one was provided)
                // we only regard an agent as better if it beats the elite agent with about 80% confidence (adjusted for multiple comparisons)
                double zScore = Utils.standardZScore(0.20, agentsInOrder.size() - 1);
                if (elites.size() == 1 && agentsInOrder.get(0) != winnersPerRun.size() - 1) {
                    // The elite agent is always the last one (and if the elite won fair and square, then we skip this
                    double eliteMean;
                    double eliteErr;
                    boolean winnerBeatsEliteBySignificantMargin;
                    // For Ordinal we want the lowest ordinal; for Win rate high is good
                    if (params.evalMethod.equals("Ordinal")) {
                        eliteMean = tournament.getOrdinalRank(winnersPerRun.size() - 1);
                        eliteErr = tournament.getOrdinalStdErr(winnersPerRun.size() - 1);
                        // The sqrt(2) is to adjust for a test on the difference between two means (assuming same n and sd)
                        winnerBeatsEliteBySignificantMargin = eliteMean - zScore * Math.sqrt(2) * eliteErr > bestResult.a.a;
                    } else {
                        eliteMean = tournament.getWinRate(winnersPerRun.size() - 1);
                        eliteErr = tournament.getWinStdErr(winnersPerRun.size() - 1);
                        winnerBeatsEliteBySignificantMargin = eliteMean + zScore * Math.sqrt(2) * eliteErr < bestResult.a.a;
                    }
                    if (!winnerBeatsEliteBySignificantMargin) {
                        if (params.verbose)
                            System.out.printf("Elite agent won with %.2f +/- %.2f versus challenger at %.2f, so we are sticking with it%n", eliteMean, eliteErr, bestResult.a.a);
                        bestResult = new Pair<>(new Pair<>(eliteMean, eliteErr), elites.get(0));
                    }
                }
            }
        } else {
            // otherwise we use the evalGames results from each run to pick the best one (which is already in bestResult)
            // However, if elites is not empty, then this means we are using the elite as the benchmark
            // so if the best evalGames result is not better than the elite, then we should use the elite.
            if (!elites.isEmpty()) {
                // we assume (As the only supported use case as of April 2025) that the last
                // element of the elite array is the one that was just used as a benchmark
                if (params.evalMethod.equals("Ordinal")) {
                    double maxLevel = (nPlayers + 1) / 2.0;
                    if (bestResult.a.a > maxLevel) {
                        // keep the elite agent as the winner
                        System.out.format("Challenger only achieved %.3f against a maximum target of %.3f. Sticking with the current benchmark.",
                                bestResult.a.a, maxLevel);
                        bestResult = Pair.of(Pair.of(maxLevel, 0.0), elites.get(0));
                    }
                } else {
                    double minLevel = 1.0 / nPlayers;
                    if (bestResult.a.a < minLevel) {
                        // keep the elite agent as the winner
                        System.out.format("Challenger only achieved %.3f against a minimum target of %.3f. Sticking with the current benchmark.",
                                bestResult.a.a, minLevel);
                        bestResult = Pair.of(Pair.of(minLevel, 0.0), elites.get(0));
                    }
                }
            }
        }
        // Now we optionally apply one-step deviations to the best result
        if (params.OSDBudget > 0) {
            NTBEAParameters OSDParams = (NTBEAParameters) params.copy();
            // we take the OSDBudget and split it across 10 iterations
            // with a minimum of 1000 games per iteration
            OSDParams.repeats = Math.max(Math.min(10, OSDParams.OSDBudget / 1000), 1);
            OSDParams.tournamentGames = params.OSDBudget / OSDParams.repeats;
            OneStepDeviations OSD = new OneStepDeviations(OSDParams, evaluator);
            // now tweak the final settings
            bestResult.b = OSD.run(bestResult.b);
        }

        if (params.verbose) {
            System.out.println("\nFinal Recommendation: ");
            // we don't log the final run to file to avoid duplication
            printDetailsOfRun(bestResult);
        }
        if (params.searchSpace instanceof ITPSearchSpace<?> itp) {
            itp.writeAgentJSON(bestResult.b, finalFilename());
            return new Pair<>(itp.instantiate(bestResult.b), bestResult.b);
        }
        return new Pair<>(null, bestResult.b);
    }

    public final String finalFilename() {
        return params.destDir + File.separator + "Recommended_Final.json";
    }

    public boolean hasAlreadyRun() {
        File f = new File(finalFilename());
        return f.exists();
    }

    protected void runTrials() {
        evaluator.reset();
        searchFramework.runTrial(evaluator, params.iterationsPerRun);
    }

    protected void runIteration() {
        landscapeModel.reset();

        runTrials();

        if (params.verbose)
            landscapeModel.logResults(params);

        int[] thisWinnerSettings = landscapeModel.getBestSampled();

        // now run the evaluation games on the final recommendation (if any...if not we report the NTBEA landscape estimate)
        Pair<Double, Double> scoreOfBestAgent = params.evalGames == 0
                ? new Pair<>(landscapeModel.getMeanEstimate(landscapeModel.getBestSampled()), 0.0)
                : evaluateWinner(thisWinnerSettings);

        if (params.searchSpace instanceof ITPSearchSpace<?> itp) {
            winnersPerRun.add(itp.instantiate(thisWinnerSettings));
        }
        winnerSettings.add(thisWinnerSettings);
        Pair<Pair<Double, Double>, int[]> resultToReport = new Pair<>(scoreOfBestAgent, thisWinnerSettings);
        if (params.verbose)
            printDetailsOfRun(resultToReport);
        if (!params.logFile.isEmpty()) {
            logSummary(resultToReport, params);
        }
        if (resultToReport.a.a > bestResult.a.a)
            bestResult = resultToReport;
    }

    private List<IGameListener> createListeners() {
        List<IGameListener> retValue = params.listenerClasses.stream().map(IGameListener::createListener).collect(Collectors.toList());
        List<String> directories = Arrays.asList(params.destDir.split(Pattern.quote(File.separator)));
        retValue.forEach(l -> l.setOutputDirectory(directories.toArray(new String[0])));
        return retValue;
    }

    protected Pair<Double, Double> evaluateWinner(int[] winnerSettings) {

        double[] results = IntStream.range(0, params.evalGames)
                .mapToDouble(answer -> evaluator.evaluate(winnerSettings)).toArray();
        Arrays.sort(results);
        double avg = Arrays.stream(results).average().orElse(0.0);
        double stdErr = Math.sqrt(Arrays.stream(results).map(d -> Math.pow(d - avg, 2.0)).sum()) / (params.evalGames - 1.0);
        return new Pair<>(avg, stdErr);
    }

    /**
     * This just prints out some useful info on the NTBEA results. It lists the full underlying recommended
     * parameter settings, and the estimated mean score of these (with std error).
     *
     * @param data The results of the NTBEA trials.
     *             The {@code Pair<Double, Double>} is the mean and std error on the mean for the final recommendation,
     *             as calculated from the post-NTBEA evaluation trials.
     *             The double[] is the best sampled settings from the main NTBEA trials (that are then evaluated to get
     *             a more accurate estimate of their utility).
     */
    protected void printDetailsOfRun(Pair<Pair<Double, Double>, int[]> data) {
        System.out.printf("Recommended settings have score %.3g +/- %.3g:\t%s\n %s%n",
                data.a.a, data.a.b,
                Arrays.stream(data.b).mapToObj(it -> String.format("%d", it)).collect(joining(", ")),
                IntStream.range(0, data.b.length).mapToObj(i -> new Pair<>(i, data.b[i]))
                        .map(p -> String.format("\t%s:\t%s\n", params.searchSpace.name(p.a), valueToString(p.a, p.b, params.searchSpace)))
                        .collect(joining(" ")));
    }

    public static void logSummary(Pair<Pair<Double, Double>, int[]> data, NTBEAParameters params) {
        try {
            Utils.createDirectory(params.destDir);
            File log = new File(params.destDir.isEmpty() ? params.logFile : params.destDir + File.separator + params.logFile);
            boolean fileExists = log.exists();
            FileWriter writer = new FileWriter(log, true);
            // if logFile does not yet exist, write a header line first
            if (!fileExists) {
                List<String> headers = new ArrayList<>();
                headers.addAll(Arrays.asList("Estimate", "Other"));
                headers.addAll(IntStream.range(0, params.searchSpace.nDims()).mapToObj(params.searchSpace::name).toList());
                writer.write(String.join("\t", headers) + "\n");
            }
            String firstPart = String.format("%.4g\t%.4g\t", data.a.a, data.a.b);
            String values = IntStream.range(0, data.b.length).mapToObj(i -> new Pair<>(i, data.b[i]))
                    .map(p -> valueToString(p.a, p.b, params.searchSpace))
                    .collect(joining("\t"));
            writer.write(firstPart + values + "\n");
            writer.flush();
            writer.close();


        } catch (IOException e) {
            System.out.println(e.getMessage() + " : Error accessing file " + params.logFile);
        }
    }

    private static String valueToString(int paramIndex, int valueIndex, SearchSpace ss) {
        Object value = ss.value(paramIndex, valueIndex);
        String valueString = value.toString();
        if (value instanceof Integer) {
            valueString = String.format("%d", value);
        } else if (value instanceof Double) {
            valueString = String.format("%.3g", value);
        }
        return valueString;
    }

    public void writeAgentJSON(int[] currentBestSettings, String s) {
        if (params.searchSpace instanceof ITPSearchSpace<?> itp) {
            itp.writeAgentJSON(currentBestSettings, s);
        } else {
            throw new AssertionError("Cannot write agent JSON for non-ITP search space");
        }
    }
}
