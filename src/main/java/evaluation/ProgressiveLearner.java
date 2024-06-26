package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.*;
import evaluation.listeners.*;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import evaluation.tournaments.RandomRRTournament;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import org.apache.commons.io.FileUtils;
import players.PlayerFactory;
import players.decorators.EpsilonRandom;
import players.learners.AbstractLearner;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static evaluation.tournaments.AbstractTournament.TournamentMode.SELF_PLAY;
import static utilities.JSONUtils.loadClassFromFile;
import static utilities.Utils.getArg;

public class ProgressiveLearner {

    GameType gameToPlay;
    String dataDir, player, heuristic;
    AbstractParameters params;
    List<AbstractPlayer> agents;
    EpsilonRandom randomExplorer;
    ILearner learner;
    FeatureListener listener;
    int nPlayers, matchups, iterations, iter, finalMatchups;
    double maxExplore;
    AbstractPlayer basePlayer;
    AbstractPlayer[] agentsPerGeneration;
    String[] dataFilesByIteration;
    String[] learnedFilesByIteration;
    String prefix;
    int elite;
    boolean verbose;
    List<Integer> currentElite = new ArrayList<>();

    public ProgressiveLearner(String[] args) {

        /* 1. Settings for the tournament */

        try {
            gameToPlay = GameType.valueOf(getArg(args, "game", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("A Game must be specified");
        }

        nPlayers = getArg(args, "nPlayers", 2);
        matchups = getArg(args, "matchups", 1);
        finalMatchups = getArg(args, "finalMatchups", 1000);
        iterations = getArg(args, "iterations", 100);
        maxExplore = getArg(args, "explore", 0.0);
        verbose = getArg(args, "verbose", false);
        elite = getArg(args, "elite", iterations + 1);
        agentsPerGeneration = new AbstractPlayer[iterations];
        dataFilesByIteration = new String[iterations];
        String learnerDefinition = getArg(args, "learner", "");
        if (learnerDefinition.equals(""))
            throw new IllegalArgumentException("Must specify a learner file");
        learner = loadClassFromFile(learnerDefinition);
        String listenerDefinition = getArg(args, "listener", "");
        if (listenerDefinition.equals(""))
            throw new IllegalArgumentException("Must specify a listener file");
        listener = loadClassFromFile(listenerDefinition);
        prefix = getArg(args, "prefix", "ProgLearn");

        learnedFilesByIteration = new String[iterations];
        player = getArg(args, "player", "");
        String gameParams = getArg(args, "gameParams", "");
        dataDir = getArg(args, "dir", "");

        params = AbstractParameters.createFromFile(gameToPlay, gameParams);

        heuristic = getArg(args, "heuristic", "");
    }

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h") || argsList.isEmpty()) {
            System.out.println(
                    "There are a number of possible arguments:\n" +
                            "\tgame=          The name of the game to play. Required. \n" +
                            "\tnPlayers=      The number of players in each game. Defaults to the minimum for the game.\n" +
                            "\tplayer=        The JSON file of the agent definition to be used. \n" +
                            "\t               This will need to use a heuristic that takes a file input.\n" +
                            "\t               This location(s) for this injection in the JSON file must be marked with '*FILE*'\n" +
                            "\t               The content of the 'heuristic' argument will be injected to replace *HEURISTIC* in the file.\n" +
                            "\t               It can also optionally have the class to be used as FeatureVector marked with '*PHI*'\n" +
                            "\t               in which case the value specified in the statePhi argument will be injected.\n" +
                            "\tlistener=      A JSON file that contains the definition to be used for the FeatureListener.\n" +
                            "\tlearner=       The JSON file that specifies an ILearner implementation.\n" +
                            "\t               This learner must be compatible with the heuristic - in that it must \n" +
                            "\t               generate a file that the heuristic can read.\n" +
                            "\theuristic=     (optional) Class name that specifies the heuristic to be injected into Agents as described above.\n" +
                            "\tprefix=        Name to use as output directory.\n" +
                            "\texplore=       The starting exploration rate - at which random actions are taken by agents.\n" +
                            "\t               This will reduce linearly to zero for the final iteration.\n" +
                            "\tdir=           The directory containing agent JSON files for learned heuristics and raw data\n" +
                            "\tgameParams=    (Optional) A JSON file from which the game parameters will be initialised.\n" +
                            "\tmatchups=      Defaults to 1. The number of games to play before the learning process is called.\n" +
                            "\titerations=    Stop after this number of learning iterations. Defaults to 100.\n" +
                            "\tfinalMatchups= The number of games to run in a final tournament between all agents. Defaults to 1000.\n" +
                            "\telite=         The number of agents to keep in the tournament. Defaults to iterations.\n"
            );
            return;
        }

        ProgressiveLearner pl = new ProgressiveLearner(args);

        /*
        Once we train a heuristic we need to inject it in a standard fashion into the agents
        A flexible way to do this is to replace placeholders in the JSON file with the details of the heuristic;
         - class
         - constructor parameters (will need a file as a standard here)

         Then I insert the whole JSONObject - class name, plus the assumption of a single argument for a config file
         which is what we are learning. The name of this file can be controlled from here - say ILearner name, date, iteration
         The ILearner will generate the file - the idea is that we decide what data to use here; load it into memory, pass this
         to the ILearner, along with the name of the file it should create with the results.
         For the moment we will use a synchronous, single-threaded method, and wait between game runs.
         */

        pl.run();
    }

    public void run() {
        iter = 0;
        do {
            loadAgents();

            runGamesWithAgents();

            learnFromNewData();
            iter++;
        } while (iter < iterations);

        // Now we can run a tournament of everyone
        List<AbstractPlayer> finalAgents = Arrays.stream(agentsPerGeneration).collect(Collectors.toList());
        finalAgents.add(basePlayer);
        finalAgents.forEach(AbstractPlayer::clearDecorators); // remove any random moves
        Map<RunArg, Object> config = new HashMap<>();
        config.put(RunArg.matchups, finalMatchups);
        config.put(RunArg.seed, System.currentTimeMillis());
        config.put(RunArg.byTeam, false);
        RoundRobinTournament tournament = new RandomRRTournament(finalAgents, gameToPlay, nPlayers, params, SELF_PLAY,
                config);

        tournament.setListeners(new ArrayList<>());
        tournament.run();
        int winnerIndex = tournament.getWinnerIndex();
        if (winnerIndex != finalAgents.size() - 1) {
            // if the basePlayer won, then meh!
            // In this case we need to check whether the winning file is a single file or a directory
            File winningLocation = new File(learnedFilesByIteration[winnerIndex]);
            if (winningLocation.exists() && winningLocation.isDirectory()) {
                try {
                    FileUtils.copyDirectory(winningLocation, new File(String.format("%s_OverallWinner", prefix)));
                } catch (IOException e) {
                    System.out.println("Error copying the final winning heuristic directory");
                    e.printStackTrace();
                }
            } else {
                File winningFile = new File(learnedFilesByIteration[winnerIndex] + ".txt");
                // just copy the file
                if (winningFile.exists()) {
                    String fileName = String.format("%s_OverallWinner.txt", prefix);
                    try {
                        FileUtils.copyFile(winningFile, new File(fileName));
                    } catch (IOException e) {
                        System.out.println("Error copying the final winning heuristic");
                        e.printStackTrace();
                    }
                } else
                    throw new AssertionError("No winning heuristic file found " + winningFile.getName());
            }
        }
    }

    private List<Integer> topNAgents(RoundRobinTournament tournament, int N) {
        return IntStream.range(0, tournament.getNumberOfAgents())
                .mapToObj(i -> new Pair<>(i, tournament.getOrdinalRank(i)))
                .sorted(Comparator.comparingDouble(p -> p.b))   // lower rank is better
                .limit(N).map(p -> p.a).collect(Collectors.toList());
    }

    private void loadAgents() {
        agents = new LinkedList<>();
        File playerLoc = new File(player);
        if (player.isEmpty())
            throw new IllegalArgumentException("No player file specified");
        if (playerLoc.isDirectory()) {
            throw new IllegalArgumentException("Not yet implemented for a directory of players");
        }
        if (iter == 0) {
            String fileName = learnedFilesByIteration[iter] == null ? "" : learnedFilesByIteration[iter];
            agents.add(PlayerFactory.createPlayer(player, rawJSON -> injectAgentAttributes(rawJSON, fileName)));
            basePlayer = agents.get(0);
            basePlayer.setName("Default Agent");
        } else {
            agents.add(basePlayer);
            agents.addAll(Arrays.asList(agentsPerGeneration).subList(0, iter));
        }

        randomExplorer = new EpsilonRandom();
        agents.forEach(a -> a.addDecorator(randomExplorer));
    }

    private String injectAgentAttributes(String rawJSON, String fileName) {
        return listener.injectAgentAttributes(rawJSON.replaceAll(Pattern.quote("*FILE*"), fileName)
                .replaceAll(Pattern.quote("*HEURISTIC*"), heuristic));
    }

    private void runGamesWithAgents() {
        // Run!
        // First we only put the elite agents into the tournament, and track their numbers
        if (currentElite.isEmpty()) {
            currentElite = IntStream.range(0, agents.size()).boxed().collect(Collectors.toList());
        }
        List<AbstractPlayer> agentsToPlay = currentElite.stream().map(i -> agents.get(i)).collect(Collectors.toList());
        Map<RunArg, Object> config = new HashMap<>();
        config.put(RunArg.matchups, finalMatchups);
        config.put(RunArg.seed, System.currentTimeMillis());
        config.put(RunArg.byTeam, false);
        RoundRobinTournament tournament = new RandomRRTournament(agentsToPlay, gameToPlay, nPlayers, params, SELF_PLAY,
                config);
        tournament.verbose = false;
        double exploreEpsilon = maxExplore * (iterations - iter - 1) / (iterations - 1);
        System.out.println("Explore = " + exploreEpsilon);
        randomExplorer.setEpsilon(exploreEpsilon);

        String fileName = String.format("%s_%d.data", prefix, iter);
        dataFilesByIteration[iter] = fileName;
        listener.setLogger(new FileStatsLogger(fileName, "\t", false));
        tournament.setListeners(Collections.singletonList(listener));
        tournament.run();

        if (verbose) {
            for (int i = 0; i < agentsToPlay.size(); i++) {
                System.out.printf("Agent: %d %s\twins %.2f +/- %.3f\tOrd %.2f +/- %.2f%n", i, agentsToPlay.get(i).toString(),
                        tournament.getWinRate(i), tournament.getWinStdErr(i), tournament.getOrdinalRank(i), tournament.getOrdinalStdErr(i));
            }
        }
        List<Integer> eliteIndices = topNAgents(tournament, elite); // these are the indices within currentElite
        List<Integer> newElite = eliteIndices.stream().map(i -> currentElite.get(i))
                .collect(Collectors.toList());
        if (verbose) {
            System.out.println("Current elite = " + Arrays.toString(currentElite.toArray()));
            System.out.println("Elite indices = " + Arrays.toString(eliteIndices.toArray()));
            System.out.println("Elite agents  = " + Arrays.toString(newElite.toArray()));
        }
        if (elite < agentsToPlay.size()) {
            // we need to select the elite agents
            Set<Integer> removedAgents = new HashSet<>(currentElite);
            for (Integer i : newElite)
                removedAgents.remove(i); // generate a list of agents that were elite but now aren't
            if (verbose) {
                System.out.println("Elite agents = " + Arrays.toString(newElite.toArray()));
                System.out.println("Removed agents = " + Arrays.toString(removedAgents.toArray()));
            }
            currentElite = newElite;
        }
        currentElite.add(iter + 1); // add the new agent
    }

    private void learnFromNewData() {
        // for the moment we will just supply the most recent file
        learner.learnFrom(dataFilesByIteration[iter]);

        String iterationPrefix = String.format("%s_%d", prefix, iter);
        learnedFilesByIteration[iter] = iterationPrefix;
        learner.writeToFile(iterationPrefix);

        // if we only have one agent type, then we can create one agent as the result of this round
        agentsPerGeneration[iter] = PlayerFactory.createPlayer(player, rawJSON -> injectAgentAttributes(rawJSON, iterationPrefix));
        agentsPerGeneration[iter].setName(String.format("Iteration %2d", iter + 1));
        agentsPerGeneration[iter].addDecorator(randomExplorer);
    }
}
