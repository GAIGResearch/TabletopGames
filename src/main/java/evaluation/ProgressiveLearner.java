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
import utilities.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static evaluation.tournaments.AbstractTournament.TournamentMode.SELF_PLAY;
import static utilities.Utils.getArg;

public class ProgressiveLearner {

    GameType gameToPlay;
    String dataDir, player, defaultHeuristic, heuristic;
    AbstractParameters params;
    List<AbstractPlayer> agents;
    EpsilonRandom randomExplorer;
    ILearner learner;
    int nPlayers, matchups, iterations, iter, finalMatchups;
    double maxExplore;
    AbstractPlayer basePlayer;
    AbstractPlayer[] agentsPerGeneration;
    String[] dataFilesByIteration;
    String[] learnedFilesByIteration;
    IStateFeatureVector phi;
    Event.GameEvent frequency;
    boolean currentPlayerOnly;
    String phiClass, prefix;
    boolean useOnlyLast;

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
        useOnlyLast = getArg(args, "useOnlyLast", false);
        maxExplore = getArg(args, "explore", 0.0);
        agentsPerGeneration = new AbstractPlayer[iterations];
        dataFilesByIteration = new String[iterations];
        String learnerClass = getArg(args, "learner", "");
        if (learnerClass.equals(""))
            throw new IllegalArgumentException("Must specify a learner class");
        learner = Utils.loadClassFromString(learnerClass);
        if (learner instanceof AbstractLearner) {
            ((AbstractLearner) learner).setGamma(getArg(args, "gamma", 1.0));
            ((AbstractLearner) learner).setTarget(getArg(args, "target", AbstractLearner.Target.WIN));
        }

        learnedFilesByIteration = new String[iterations];
        player = getArg(args, "player", "");
        String gameParams = getArg(args, "gameParams", "");
        dataDir = getArg(args, "dir", "");

        params = AbstractParameters.createFromFile(gameToPlay, gameParams);

        phiClass = getArg(args, "statePhi", "");
        if (phiClass.equals(""))
            throw new IllegalArgumentException("Must specify a state feature vector");
        phi = Utils.loadClassFromString(phiClass);
        prefix = getArg(args, "fileName", String.format("%tF-%s", System.currentTimeMillis(), phi.getClass().getSimpleName()));
        defaultHeuristic = getArg(args, "defaultHeuristic", "players.heuristics.NullHeuristic");
        heuristic = getArg(args, "heuristic", "players.heuristics.LinearStateHeuristic");
        currentPlayerOnly = getArg(args, "stateCPO", false);
        frequency = Event.GameEvent.valueOf(getArg(args, "stateFreq", "ACTION_TAKEN"));
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
                            "\t               A default heuristic can be specified for the initial iteration with '*DEFAULT*'\n" +
                            "\t               in which case the defaultHeuristic argument will be used.\n" +
                            "\tfileName=      The prefix to use on the files generate on each learning iteration.\n" +
                            "\t               The default will use the name of the learner and the system date.\n" +
                            "\tlearner=       The full class name of an ILearner implementation.\n" +
                            "\t               This learner must be compatible with the heuristic - in that it must \n" +
                            "\t               generate a file that the heuristic can read.\n" +
                            "\ttarget=        The target to use (WIN, ORDINAL, SCORE, WIN_MEAN, ORD_MEAN, ORD_MEAN_SCALE, ORD_SCALE, SCORE_DELTA)\n" +
                            "\tgamma=         The discount factor to use - this is applied per round, not per action\n" +
                            "\texplore=       The starting exploration rate - at which random actions are taken by agents.\n" +
                            "\t               This will reduce linearly to zero for the final iteration.\n" +
                            "\tdir=           The directory containing agent JSON files for learned heuristics and raw data\n" +
                            "\tgameParams=    (Optional) A JSON file from which the game parameters will be initialised.\n" +
                            "\tmatchups=      Defaults to 1. The number of games to play before the learning process is called.\n" +
                            "\tstatePhi=      The full class name of an IStateFeatureVector implementation that defines the inputs \n" +
                            "\t               to the heuristic used in the player files.\n" +
                            "\tstateFreq=     How frequently to record a value to regress against (ROUND_OVER, TURN_OVER, ACTION_CHOSEN, ACTION_TAKEN)\n" +
                            "\t               Defaults to ACTION_TAKEN.\n" +
                            "\tstateCPO=      Whether to only record states for the current player (defaults to all players)\n" +
                            "\theuristic=     A class name for a heuristic to inject into the Agent JSON definition (see 'player')\n" +
                            "\t               Defaults to players.heuristics.LinearStateHeuristic\n" +
                            "\tdefaultHeuristic=Defaults to a null heuristic (random play). This is only used in the first iteration\n" +
                            "\t               when we have no data.  \n" +
                            "\titerations=    Stop after this number of learning iterations. Defaults to 100.\n" +
                            "\tfinalMatchups= The number of games to run in a final tournament between all agents. Defaults to 1000.\n"
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
        RoundRobinTournament tournament = new RandomRRTournament(finalAgents, gameToPlay, nPlayers,  SELF_PLAY, finalMatchups,
                finalMatchups, System.currentTimeMillis(), params);

        tournament.setListeners(new ArrayList<>());
        tournament.runTournament();
        int winnerIndex = tournament.getWinnerIndex();
        if (winnerIndex != finalAgents.size() - 1) {
            // if the basePlayer won, then meh!
            String fileName = String.format("%s_Winner.txt", prefix);
            try {
                FileUtils.copyFile(new File(learnedFilesByIteration[winnerIndex]), new File(fileName));
            } catch (IOException e) {
                System.out.println("Error copying the final winning heuristic");
                e.printStackTrace();
            }
        }
    }

    private void loadAgents() {
        agents = new LinkedList<>();
        File playerLoc = new File(player);
        if (player.isEmpty())
            throw new IllegalArgumentException("No player file specified");
        if (playerLoc.isDirectory()) {
            throw new IllegalArgumentException("Not yet implemented for a directory of players");
        }
        if (iter == 0 || useOnlyLast) {
            String fileName = learnedFilesByIteration[iter] == null ? "" : learnedFilesByIteration[iter] ;
            agents.add(PlayerFactory.createPlayer(player, rawJSON -> injectAgentAttributes(rawJSON, fileName)));
            if (iter == 0) {
                basePlayer = agents.get(0);
                basePlayer.setName("Default Agent");
            }
        } else {
            agents.add(basePlayer);
            agents.addAll(Arrays.asList(agentsPerGeneration).subList(0, iter));
        }
        randomExplorer = new EpsilonRandom();
        agents.forEach(a -> a.addDecorator(randomExplorer));
    }

    private String injectAgentAttributes(String raw, String file) {
        return raw.replaceAll(Pattern.quote("*FILE*"), file)
                .replaceAll(Pattern.quote("*PHI*"), phiClass)
                .replaceAll(Pattern.quote("*HEURISTIC*"), heuristic)
                .replaceAll(Pattern.quote("*DEFAULT*"), defaultHeuristic);
    }

    private void runGamesWithAgents() {
        // Run!
        RoundRobinTournament tournament = new RandomRRTournament(agents, gameToPlay, nPlayers,  SELF_PLAY, matchups,
                matchups, System.currentTimeMillis(), params);
        tournament.verbose = false;
        double exploreEpsilon = maxExplore * (iterations - iter - 1) / (iterations - 1);
        System.out.println("Explore = " + exploreEpsilon);
        randomExplorer.setEpsilon(exploreEpsilon);

        String fileName = String.format("%s_%d.data", prefix, iter);
        dataFilesByIteration[iter] = fileName;
        StateFeatureListener dataTracker = new StateFeatureListener(new FileStatsLogger(fileName), phi, frequency, currentPlayerOnly);
        tournament.setListeners(Collections.singletonList(dataTracker));
        tournament.runTournament();
    }

    private void learnFromNewData() {
        // for the moment we will just supply the most recent file
        learner.learnFrom(dataFilesByIteration[iter]);

        String fileName = String.format("%s_%d.txt", prefix, iter);
        learnedFilesByIteration[iter] = fileName;
        learner.writeToFile(fileName);

        // if we only have one agent type, then we can create one agent as the result of this round
        agentsPerGeneration[iter] = PlayerFactory.createPlayer(player, rawJSON -> injectAgentAttributes(rawJSON, fileName));
        agentsPerGeneration[iter].setName(String.format("Iteration %2d", iter + 1));
        agentsPerGeneration[iter].addDecorator(randomExplorer);
    }
}
