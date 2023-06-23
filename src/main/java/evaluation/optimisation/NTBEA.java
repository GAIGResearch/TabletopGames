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
import ntbea.NTupleBanditEA;
import players.PlayerFactory;
import utilities.Pair;
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
import static utilities.Utils.getArg;

public class NTBEA {

    NTBEAParameters params;
    BanditLandscapeModel landscapeModel;
    ITPSearchSpace searchSpace;
    NTupleBanditEA searchFramework;

    public NTBEA(BanditLandscapeModel landscapeModel, NTBEAParameters parameters) {
        this.landscapeModel = landscapeModel;
        searchSpace = (ITPSearchSpace) landscapeModel.getSearchSpace();
        this.params = parameters;
        searchFramework = new NTupleBanditEA(landscapeModel, params.kExplore, params.neighbourhoodSize);
    }

    public void run() {

        int searchSpaceSize = IntStream.range(0, searchSpace.nDims()).reduce(1, (acc, i) -> acc * searchSpace.nValues(i));

        // Set up opponents
        List<AbstractPlayer> opponents = new ArrayList<>();
        // if we are in coop mode, then we have no opponents. This is indicated by leaving the list empty.
        if (!params.opponentDescriptor.equals("coop")) {
            // first check to see if we have a directory or not
            opponents = PlayerFactory.createPlayers(params.opponentDescriptor);
        }

        IGameHeuristic gameHeuristic = null;
        IStateHeuristic stateHeuristic = null;
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

    protected void runIteration() {

    }

    protected void runTrial() {

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

}
