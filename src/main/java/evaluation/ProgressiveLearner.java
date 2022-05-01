package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.ParameterFactory;
import core.interfaces.IActionFeatureVector;
import core.interfaces.ILearner;
import core.interfaces.IStateFeatureVector;
import games.GameType;
import players.PlayerFactory;
import utilities.FileStatsLogger;
import utilities.StateFeatureListener;
import utilities.Utils;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static utilities.Utils.getArg;

public class ProgressiveLearner {

    GameType gameToPlay;
    String dataDir, player;
    AbstractParameters params;
    List<AbstractPlayer> agents;
    ILearner learner;
    int nPlayers, matchups, iterations, iter;
    AbstractPlayer[] agentsPerGeneration;
    IStateFeatureVector phi;

    public ProgressiveLearner(String[] args) {

        /* 1. Settings for the tournament */

        try {
            gameToPlay = GameType.valueOf(getArg(args, "game", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("A Game must be specified");
        }

        nPlayers = getArg(args, "nPlayers", 2);
        matchups = getArg(args, "matchups", 1);
        iterations = getArg(args, "iterations", 100);
        agentsPerGeneration = new AbstractPlayer[iterations];
        player = getArg(args, "player", "");
        String gameParams = getArg(args, "gameParams", "");
        dataDir = getArg(args, "dir", "");

        params = ParameterFactory.createFromFile(gameToPlay, gameParams);
        String learnerClass = getArg(args, "learner", "");
        if (learnerClass.equals(""))
            throw new IllegalArgumentException("Must specify a learner class");
        learner = Utils.loadClassFromString(learnerClass);

        String phiClass = getArg(args, "statePhi", "");
        if (phiClass.equals(""))
            throw new IllegalArgumentException("Must specify a state feature vector");
        phi = Utils.loadClassFromString(phiClass);

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
                            "\t               This location(s) for this injection in the JSON file must be marked with '*HEURISTIC*'\n" +
                            "\tlearner=       The full class name of an ILearner implementation.\n" +
                            "\t               This learner must be compatible with the heuristic - in that it must \n" +
                            "\t               generate a file that the heuristic can read.\n" +
                            "\tdir=           The directory containing agent JSON files for learned heuristics and raw data\n" +
                            "\tgameParams=    (Optional) A JSON file from which the game parameters will be initialised.\n" +
                            "\tmatchups=      Defaults to 1. The number of games to play before the learning process is called.\n" +
                            "\tstatePhi=      The full class name of an IStateFeatureVector implementation that defines the inputs \n" +
                            "\t               to the heuristic used in the player files.\n" +
                            "\titerations=    Stop after this number of learning iterations. Defaults to 100."
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
    }

    private void loadAgents() {
        // For the moment we always use the previous agent - so this is brittle self-play - to be changed later
        String fileName = iter == 0 ? "" : String.format("%tF-%s_%d.txt", System.currentTimeMillis(), learner.getClass().getSimpleName(), iter);
        agents = new LinkedList<>();
        File playerLoc = new File(player);
        if (playerLoc.isDirectory()) {
            agents.addAll(PlayerFactory.createPlayers(player, s -> s.replaceAll(Pattern.quote("*HEURISTIC*"), fileName)));
        } else {
            agents.add(PlayerFactory.createPlayer(player, s -> s.replaceAll(Pattern.quote("*HEURISTIC*"), fileName)));
        }
    }

    private void runGamesWithAgents() {
        // Run!
        RoundRobinTournament tournament = new RandomRRTournament(agents, gameToPlay, nPlayers, 1, true, matchups,
                System.currentTimeMillis(), params);

        String fileName = String.format("%tF-%s_%d.data", System.currentTimeMillis(), phi.getClass().getSimpleName(), iter);
        StateFeatureListener dataTracker = new StateFeatureListener(new FileStatsLogger(fileName), phi);
        tournament.listeners = Collections.singletonList(dataTracker);
        tournament.runTournament();
    }

    private void learnFromNewData() {
        // How do we know the file that has been written with data?
        // Why not just gather trajectory data in PL? (Because I also want the date available for offline learning)
        learner.learnFrom();
    }
}
