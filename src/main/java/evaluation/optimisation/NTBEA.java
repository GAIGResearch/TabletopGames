package evaluation.optimisation;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.IGameHeuristic;
import core.interfaces.IStateHeuristic;
import evaluation.RunArg;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import org.apache.commons.math3.util.CombinatoricsUtils;
import games.GameType;
import ntbea.NTupleBanditEA;
import ntbea.NTupleSystem;
import players.PlayerFactory;
import players.heuristics.OrdinalPosition;
import players.heuristics.PureScoreHeuristic;
import players.heuristics.WinOnlyHeuristic;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.StatSummary;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static evaluation.RunArg.byTeam;
import static evaluation.RunArg.matchups;
import static evaluation.tournaments.AbstractTournament.TournamentMode.NO_SELF_PLAY;
import static java.util.stream.Collectors.joining;

public class NTBEA {

    NTBEAParameters params;
    NTupleSystem landscapeModel;
    NTupleBanditEA searchFramework;
    List<Object> winnersPerRun = new ArrayList<>();
    List<int[]> winnerSettings = new ArrayList<>();
    List<int[]> elites = new ArrayList<>();
    Pair<Pair<Double, Double>, int[]> bestResult = new Pair<>(new Pair<>(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new int[0]);
    GameEvaluator evaluator;
    GameType game;
    int nPlayers;
    int currentIteration = 0;
    IStateHeuristic stateHeuristic;
    IGameHeuristic gameHeuristic;

    public NTBEA(NTBEAParameters parameters, GameType game, int nPlayers) {
        // Now initialise the other bits and pieces needed for the NTBEA package
        this.params = parameters;
        landscapeModel = new NTupleSystem(params.searchSpace);
        landscapeModel.setUse3Tuple(params.useThreeTuples);
        landscapeModel.addTuples();

        searchFramework = new NTupleBanditEA(landscapeModel, params.kExplore, params.neighbourhoodSize);
        this.game = game;
        this.nPlayers = nPlayers;
        // Set up opponents
        // if we are in coop mode, then we have no opponents. This is indicated by leaving the list empty.
        List<AbstractPlayer> opponents = params.mode == NTBEAParameters.Mode.CoopNTBEA ? new ArrayList<>()
                : PlayerFactory.createPlayers(params.opponentDescriptor);

        if (params.tuningGame) {
            if (new File(params.evalMethod).exists()) {
                // load from file
                gameHeuristic = JSONUtils.loadClassFromFile(params.evalMethod);
            } else {
                if (params.evalMethod.contains(".json"))
                    throw new AssertionError("File not found : " + params.evalMethod);
                try {
                    Class<?> evalClass = Class.forName("evaluation.heuristics." + params.evalMethod);
                    gameHeuristic = (IGameHeuristic) evalClass.getConstructor().newInstance();
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("evaluation.heuristics." + params.evalMethod + " not found");
                } catch (NoSuchMethodException e) {
                    throw new AssertionError("evaluation.heuristics." + params.evalMethod + " has no no-arg constructor");
                } catch (ReflectiveOperationException e) {
                    e.printStackTrace();
                    throw new AssertionError("evaluation.heuristics." + params.evalMethod + " reflection error");
                }
            }

        } else {
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
                params.searchSpace,
                params.gameParams,
                nPlayers,
                opponents,
                params.seed,
                stateHeuristic,
                gameHeuristic,
                true
        );
    }

    public void setOpponents(List<AbstractPlayer> opponents) {
        evaluator.opponents = opponents;
    }

    public void addElite(int[] settings) {
        // We use these settings as players in our final tournament
        elites.add(settings);
    }

    /**
     * This returns the optimised object, plus the settings that produced it (indices to the values in the search space)
     *
     * @return
     */
    public Pair<Object, int[]> run() {

        for (currentIteration = 0; currentIteration < params.repeats; currentIteration++) {
            runIteration();
        }

        // After all runs are complete, if tournamentGames are specified, then we allow all the
        // winners from each iteration to play in a tournament and pick the winner of this tournament
        if (params.tournamentGames > 0 && winnersPerRun.get(0) instanceof AbstractPlayer) {
            if (!elites.isEmpty()) {
                // first of all we add the elites into winnerSettings, and winnersPerRun
                // i.e. we effectively add an extra 'run' for each elite
                for (int[] elite : elites) {
                    winnerSettings.add(elite);
                    winnersPerRun.add(params.searchSpace.getAgent(elite));
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
            //double combinationsOfPlayers = CombinatoricsUtils.binomialCoefficientDouble(players.size(), nPlayers);
            int nTeams = params.byTeam ? game.createGameInstance(nPlayers).getGameState().getNTeams() : nPlayers;
            if (players.size() < nTeams) {
                System.out.println("Not enough players to run a tournament with " + nTeams + " players. Skipping the final tournament - " +
                        "check the repeats options is at least equal to the number of players.");
            } else {
                long permutationsOfPlayers = CombinatoricsUtils.factorial(players.size()) / CombinatoricsUtils.factorial(players.size() - nTeams);
                int gamesPerMatchup = (int) Math.ceil((double) params.tournamentGames / permutationsOfPlayers);  // we round up.
                if (params.verbose)
                    System.out.printf("Running %d games per matchup, %d total games, %d permutations%n",
                            gamesPerMatchup, gamesPerMatchup * permutationsOfPlayers, permutationsOfPlayers);
                Map<RunArg, Object> config = new HashMap<>();
                config.put(matchups, gamesPerMatchup);
                config.put(byTeam, false);
                config.put(RunArg.distinctRandomSeeds, 0);
                RoundRobinTournament tournament = new RoundRobinTournament(players, game, nPlayers, params.gameParams,
                        NO_SELF_PLAY, config);
                tournament.verbose = false;
                createListeners().forEach(tournament::addListener);
                tournament.run();
                // create a new list of results in descending order of score
                IntToDoubleFunction cmp = params.evalMethod.equals("Ordinal") ? i -> -tournament.getOrdinalRank(i) : tournament::getWinRate;
                List<Integer> agentsInOrder = IntStream.range(0, players.size())
                        .boxed()
                        .sorted(Comparator.comparingDouble(cmp::applyAsDouble))
                        .collect(Collectors.toList());
                Collections.reverse(agentsInOrder);
                params.logFile = "RRT_" + params.logFile;
                for (int index : agentsInOrder) {
                    if (params.verbose)
                        System.out.printf("Player %d %s\tWin Rate: %.3f +/- %.3f\tMean Ordinal: %.2f +/- %.2f%n", index, Arrays.toString(winnerSettings.get(index)),
                                tournament.getWinRate(index), tournament.getWinStdErr(index),
                                tournament.getOrdinalRank(index), tournament.getOrdinalStdErr(index));
                    Pair<Double, Double> resultToReport = new Pair<>(tournament.getWinRate(index), tournament.getWinStdErr(index));
                    if (params.evalMethod.equals("Ordinal"))
                        resultToReport = new Pair<>(tournament.getOrdinalRank(index), tournament.getOrdinalStdErr(index));

                    logSummary(new Pair<>(resultToReport, winnerSettings.get(index)), params);
                }
                params.logFile = params.logFile.substring(4);
                bestResult = params.evalMethod.equals("Ordinal") ?
                        new Pair<>(new Pair<>(tournament.getOrdinalRank(agentsInOrder.get(0)), tournament.getOrdinalStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0))) :
                        new Pair<>(new Pair<>(tournament.getWinRate(agentsInOrder.get(0)), tournament.getWinStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0)));

            // We then want to check the win rate against the elite agent (if one was provided)
            // we only regard an agent as better, if it beats the elite agent by at least 2 sd (so, c. 95%) confidence
            if (elites.size() == 1 && agentsInOrder.get(0) != winnersPerRun.size() - 1) {
                // The elite agent is always the last one (and if the elite won fair and square, then we skip this
                double eliteWinRate = tournament.getWinRate(winnersPerRun.size() - 1);
                double eliteStdErr = tournament.getWinStdErr(winnersPerRun.size() - 1);
                if (eliteWinRate + 2 * eliteStdErr > bestResult.a.a) {
                    if (params.verbose)
                        System.out.printf("Elite agent won with %.3f +/- %.3f versus challenger at %.3f, so we are sticking with it%n", eliteWinRate, eliteStdErr, bestResult.a.a);
                    bestResult = new Pair<>(new Pair<>(eliteWinRate, eliteStdErr), elites.get(0));}
                }
            }
        }
        if (params.verbose) {
            System.out.println("\nFinal Recommendation: ");
            // we don't log the final run to file to avoid duplication
            printDetailsOfRun(bestResult);
        }
        return new Pair<>(params.searchSpace.getAgent(bestResult.b), bestResult.b);
    }

    protected void runTrials() {
        evaluator.reset();
        searchFramework.runTrial(evaluator, params.iterationsPerRun);
    }

    protected void runIteration() {
        landscapeModel.reset();

        runTrials();

        if (params.verbose)
            logResults();

        int[] thisWinnerSettings = Arrays.stream(landscapeModel.getBestOfSampled())
                .mapToInt(d -> (int) d)
                .toArray();

        // now run the evaluation games on the final recommendation (if any...if not we report the NTBEA landscape estimate)
        Pair<Double, Double> scoreOfBestAgent = params.evalGames == 0
                ? new Pair<>(landscapeModel.getMeanEstimate(landscapeModel.getBestOfSampled()), 0.0)
                : evaluateWinner(thisWinnerSettings);

        winnersPerRun.add(params.searchSpace.getAgent(thisWinnerSettings));
        winnerSettings.add(thisWinnerSettings);
        Pair<Pair<Double, Double>, int[]> resultToReport = new Pair<>(scoreOfBestAgent, thisWinnerSettings);
        if (params.verbose)
            printDetailsOfRun(resultToReport);
        logDetailsOfRun(resultToReport);
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

        double avg = Arrays.stream(results).average().orElse(0.0);
        double stdErr = Math.sqrt(Arrays.stream(results)
                .map(d -> Math.pow(d - avg, 2.0)).sum()) / (params.evalGames - 1.0);

        return new Pair<>(avg, stdErr);
    }


    private void logResults() {

        System.out.println("Current best sampled point (using mean estimate): " +
                Arrays.toString(landscapeModel.getBestOfSampled()) +
                String.format(", %.3g", landscapeModel.getMeanEstimate(landscapeModel.getBestOfSampled())));

        String tuplesExploredBySize = Arrays.toString(IntStream.rangeClosed(1, params.searchSpace.nDims())
                .map(size -> landscapeModel.getTuples().stream()
                        .filter(t -> t.tuple.length == size)
                        .mapToInt(it -> it.ntMap.size())
                        .sum()
                ).toArray());

        System.out.println("Tuples explored by size: " + tuplesExploredBySize);
        System.out.printf("Summary of 1-tuple statistics after %d samples:%n", landscapeModel.numberOfSamples());

        IntStream.range(0, params.searchSpace.nDims()) // assumes that the first N tuples are the 1-dimensional ones
                .mapToObj(i -> new Pair<>(params.searchSpace.name(i), landscapeModel.getTuples().get(i)))
                .forEach(nameTuplePair ->
                        nameTuplePair.b.ntMap.keySet().stream().sorted().forEach(k -> {
                            StatSummary v = nameTuplePair.b.ntMap.get(k);
                            System.out.printf("\t%20s\t%s\t%d trials\t mean %.3g +/- %.2g%n", nameTuplePair.a, k, v.n(), v.mean(), v.stdErr());
                        })
                );

        System.out.println("\nSummary of 10 most tried full-tuple statistics:");
        landscapeModel.getTuples().stream()
                .filter(t -> t.tuple.length == params.searchSpace.nDims())
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

    protected void logDetailsOfRun(Pair<Pair<Double, Double>, int[]> data) {
        if (!params.logFile.isEmpty()) {
            logSummary(data, params);
        }
    }

    private static void logSummary(Pair<Pair<Double, Double>, int[]> data, NTBEAParameters params) {
        try {
            Utils.createDirectory(params.destDir);
            File log = new File(params.destDir.isEmpty() ? params.logFile : params.destDir + File.separator + params.logFile);
            boolean fileExists = log.exists();
            FileWriter writer = new FileWriter(log, true);
            // if logFile does not yet exist, write a header line first
            if (!fileExists) {
                List<String> headers = new ArrayList<>();
                headers.addAll(Arrays.asList("estimated value", "standard error"));
                headers.addAll(params.searchSpace.getSearchKeys());
                writer.write(String.join("\t", headers) + "\n");
            }
            // then write the output
            String firstPart = String.format("%.4g\t%.4g\t", data.a.a, data.a.b);
            String values = IntStream.range(0, data.b.length).mapToObj(i -> new Pair<>(i, data.b[i]))
                    .map(p -> valueToString(p.a, p.b, params.searchSpace))
                    .collect(joining("\t"));
            writer.write(firstPart + values + "\n");
            writer.flush();
            writer.close();


        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(e.getMessage() + " : Error accessing file " + params.logFile);
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

}
