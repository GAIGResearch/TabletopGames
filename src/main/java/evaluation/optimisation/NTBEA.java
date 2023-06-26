package evaluation.optimisation;

import core.AbstractGameState;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.CoreConstants;
import core.interfaces.IGameHeuristic;
import core.interfaces.IStateHeuristic;
import core.interfaces.IStatisticLogger;
import evaluation.tournaments.RandomRRTournament;
import evodef.BanditLandscapeModel;
import evodef.EvoAlg;
import evodef.SolutionEvaluator;
import games.GameType;
import ntbea.NTupleBanditEA;
import ntbea.NTupleSystem;
import players.PlayerFactory;
import utilities.Pair;
import utilities.StatSummary;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static evaluation.tournaments.AbstractTournament.TournamentMode.NO_SELF_PLAY;
import static java.util.stream.Collectors.joining;

public class NTBEA {

    NTBEAParameters params;
    NTupleSystem landscapeModel;
    ITPSearchSpace searchSpace;
    NTupleBanditEA searchFramework;
    List<Object> winnersPerRun = new ArrayList<>();
    List<int[]> winnerSettings = new ArrayList<>();
    Pair<Pair<Double, Double>, int[]> bestResult = new Pair<>(new Pair<>(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new int[0]);
    GameEvaluator evaluator;
    AbstractParameters gameParams;
    GameType game;
    int nPlayers;
    List<AbstractPlayer> opponents;
    IGameHeuristic gameHeuristic = null;
    IStateHeuristic stateHeuristic = null;

    public NTBEA(NTupleSystem landscapeModel, NTBEAParameters parameters, GameType game, int nPlayers) {
        this.landscapeModel = landscapeModel;
        searchSpace = (ITPSearchSpace) landscapeModel.getSearchSpace();
        this.params = parameters;
        searchFramework = new NTupleBanditEA(landscapeModel, params.kExplore, params.neighbourhoodSize);
        this.game = game;
        this.nPlayers = nPlayers;
        // Set up opponents
        // if we are in coop mode, then we have no opponents. This is indicated by leaving the list empty.
        opponents = params.mode == NTBEAParameters.Mode.CoopNTBEA ? new ArrayList<>()
                : PlayerFactory.createPlayers(params.opponentDescriptor);

        if (params.tuningGame) {
            if (new File(params.evalMethod).exists()) {
                // load from file
                gameHeuristic = Utils.loadClassFromFile(params.evalMethod);
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
                stateHeuristic = (s, p) -> s.getPlayerResults()[p] == CoreConstants.GameResult.WIN_GAME ? 1.0 : 0.0;
            if (params.evalMethod.equals("Score"))
                stateHeuristic = AbstractGameState::getGameScore;
            if (params.evalMethod.equals("Heuristic"))
                stateHeuristic = AbstractGameState::getHeuristicScore;
            if (params.evalMethod.equals("Ordinal")) // we maximise, so the lowest ordinal position of 1 is best
                stateHeuristic = (s, p) -> -(double) s.getOrdinalPosition(p);
            if (stateHeuristic == null)
                throw new AssertionError("Invalid evaluation method provided: " + params.evalMethod);
        }
        // Initialise the GameEvaluator that will do all the heavy lifting
        evaluator = new GameEvaluator(
                game,
                searchSpace,
                gameParams,
                nPlayers,
                opponents,
                params.seed,
                stateHeuristic,
                gameHeuristic,
                true
        );
    }

    public void run() {

        for (int mainLoop = 0; mainLoop < params.repeats; mainLoop++) {
            runIteration();
        }

        // After all runs are complete, if tournamentGames are specified, then we allow all the
        // winners from each iteration to play in a tournament and pick the winner of this tournament
        if (params.tournamentGames > 0 && winnersPerRun.get(0) instanceof AbstractPlayer) {
            List<AbstractPlayer> players = winnersPerRun.stream().map(p -> (AbstractPlayer) p).collect(Collectors.toList());
            for (int i = 0; i < players.size(); i++) {
                players.get(i).setName(Arrays.toString(winnerSettings.get(i)));
            }
            RandomRRTournament tournament = new RandomRRTournament(players, game, nPlayers, NO_SELF_PLAY, params.tournamentGames, 0, params.seed, gameParams);
            tournament.verbose = false;
            tournament.runTournament();
            // create a new list of results in descending order of score
            IntToDoubleFunction cmp = params.evalMethod.equals("Ordinal") ? i -> -tournament.getOrdinalRank(i) : tournament::getWinRate;
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
                if (params.evalMethod.equals("Ordinal"))
                    resultToReport = new Pair<>(tournament.getOrdinalRank(index), tournament.getOrdinalStdErr(index));
                logSummary(new Pair<>(resultToReport, winnerSettings.get(index)), searchSpace, "RRT_" + params.logFile);
            }
            bestResult = params.evalMethod.equals("Ordinal") ?
                    new Pair<>(new Pair<>(tournament.getOrdinalRank(agentsInOrder.get(0)), tournament.getOrdinalStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0))) :
                    new Pair<>(new Pair<>(tournament.getWinRate(agentsInOrder.get(0)), tournament.getWinStdErr(agentsInOrder.get(0))), winnerSettings.get(agentsInOrder.get(0)));
        }
        System.out.println("\nFinal Recommendation: ");
        // we don't log the final run to file to avoid duplication
        printDetailsOfRun(bestResult, searchSpace, "");
    }

    protected void runIteration() {
        // TODO: Add GameListeners if configured
        landscapeModel.reset();

        for (int i = 0; i < params.iterationsPerRun; i++) {
            runTrial();
        }

        if (params.verbose)
            logResults();

        int[] winnerSettings = Arrays.stream(landscapeModel.getBestOfSampled())
                .mapToInt(d -> (int) d)
                .toArray();

        // now run the evaluation games on the final recommendation (if any...if not we report the NTBEA landscape estimate)
        Pair<Double, Double> scoreOfBestAgent = params.evalGame == 0
                ? new Pair<>(landscapeModel.getMeanEstimate(landscapeModel.getBestOfSampled()), 0.0)
                : evaluateWinner(winnerSettings);

        winnersPerRun.add(searchSpace.getAgent(winnerSettings));
        Pair<Pair<Double, Double>, int[]> resultToReport = new Pair<>(scoreOfBestAgent, winnerSettings);
        printDetailsOfRun(resultToReport, searchSpace, params.logFile);
        if (resultToReport.a.a > bestResult.a.a)
            bestResult = resultToReport;
    }

    protected Pair<Double, Double> evaluateWinner(int[] winnerSettings) {
        double[] results = IntStream.range(0, params.evalGame)
                .mapToDouble(answer -> evaluator.evaluate(winnerSettings)).toArray();

        double avg = Arrays.stream(results).average().orElse(0.0);
        double stdErr = Math.sqrt(Arrays.stream(results)
                .map(d -> Math.pow(d - avg, 2.0)).sum()) / (params.evalGame - 1.0);

        return new Pair<>(avg, stdErr);
    }

    protected void runTrial() {
        evaluator.reset();
        searchFramework.runTrial(evaluator, 1);
    }


    private void logResults() {

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
    public static void printDetailsOfRun(Pair<Pair<Double, Double>, int[]> data, ITPSearchSpace searchSpace, String logFile) {
        System.out.printf("Recommended settings have score %.3g +/- %.3g:\t%s\n %s%n",
                data.a.a, data.a.b,
                Arrays.stream(data.b).mapToObj(it -> String.format("%d", it)).collect(joining(", ")),
                IntStream.range(0, data.b.length).mapToObj(i -> new Pair<>(i, data.b[i]))
                        .map(p -> String.format("\t%s:\t%s\n", searchSpace.name(p.a), valueToString(p.a, p.b, searchSpace)))
                        .collect(joining(" ")));

        if (!logFile.isEmpty()) {
            logSummary(data, searchSpace, logFile);
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

}
