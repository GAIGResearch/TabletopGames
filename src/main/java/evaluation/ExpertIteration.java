package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.IActionFeatureVector;
import core.interfaces.ILearner;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IToJSON;
import evaluation.listeners.ActionFeatureListener;
import evaluation.listeners.FeatureListener;
import evaluation.listeners.StateFeatureListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.PlayerFactory;
import players.decorators.EpsilonRandom;
import utilities.JSONUtils;
import java.util.*;

import static utilities.JSONUtils.loadClass;
import static utilities.JSONUtils.loadClassFromFile;
import static utilities.Utils.getArg;

public class ExpertIteration {

    String[] originalArgs;
    GameType gameToPlay;
    String dataDir, player, heuristic;
    AbstractParameters params;
    List<AbstractPlayer> agents;
    EpsilonRandom randomExplorer;
    ILearner stateLearner, actionLearner;
    IStateFeatureVector stateFeatureVector;
    IActionFeatureVector actionFeatureVector;
    FeatureListener stateListener, actionListener;
    int nPlayers, matchups, iterations, iter, finalMatchups;
    double maxExplore;
    AbstractPlayer basePlayer;
    AbstractPlayer[] agentsPerGeneration;
    String[] stateDataFilesByIteration;
    String[] actionDataFilesByIteration;
    boolean useRounds;
    String prefix;
    int elite;
    boolean verbose;
    List<Integer> currentElite = new ArrayList<>();

    public ExpertIteration(String[] args) {

        /* 1. Settings for the tournament */

        try {
            gameToPlay = GameType.valueOf(getArg(args, "game", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("A Game must be specified");
        }

        originalArgs = args;
        nPlayers = getArg(args, "nPlayers", 2);
        matchups = getArg(args, "matchups", 1);
        finalMatchups = getArg(args, "finalMatchups", 1000);
        iterations = getArg(args, "iterations", 100);
        maxExplore = getArg(args, "explore", 0.0);
        verbose = getArg(args, "verbose", false);
        elite = getArg(args, "elite", iterations + 1);
        agentsPerGeneration = new AbstractPlayer[iterations];
        actionDataFilesByIteration = new String[iterations];
        stateDataFilesByIteration = new String[iterations];

        useRounds = getArg(args, "useRounds", false);
        if (!getArg(args, "stateFeatures", "").isEmpty()) {
            stateFeatureVector = loadClass(getArg(args, "stateFeatures", ""));
            stateListener = new StateFeatureListener(stateFeatureVector,
                    useRounds ? Event.GameEvent.ROUND_OVER : Event.GameEvent.TURN_OVER,
                    false, "dummy.txt");
            String learnerDefinition = getArg(args, "learner", "");
            if (learnerDefinition.equals(""))
                throw new IllegalArgumentException("Must specify a state learner file");
            stateLearner = loadClass(learnerDefinition);
        }
        if (!getArg(args, "actionFeatures", "").isEmpty()) {
            actionFeatureVector = loadClass(getArg(args, "actionFeatures", ""));
            actionListener = new ActionFeatureListener(actionFeatureVector, stateFeatureVector,
                    Event.GameEvent.ACTION_CHOSEN,
                    true, "dummy.txt");
            String learnerDefinition = getArg(args, "actionLearner", "");
            if (learnerDefinition.equals(""))
                throw new IllegalArgumentException("Must specify an action learner file");
            actionLearner = loadClass(learnerDefinition);
        }

        prefix = getArg(args, "prefix", "EI");

        player = getArg(args, "player", "");
        String gameParams = getArg(args, "gameParams", "");
        dataDir = getArg(args, "dir", "");

        params = AbstractParameters.createFromFile(gameToPlay, gameParams);
    }

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h") || argsList.isEmpty()) {
            System.out.println(
                    "There are a number of possible arguments:\n" +
                            "\tgame=          The name of the game to play.\n" +
                            "\tnPlayers=      The number of players in each game. Defaults to the minimum for the game.\n" +
                            "\tplayer=        The agent (or agents if a directory) that will kickstart the process. \n" +
                            "\tstateLearner=  The JSON file that specifies an ILearner implementation for an IStateFeatureVector implementation.\n" +
                            "\tactionLearner= The JSON file that specifies an ILearner implementation for an IActionFeatureVector implementation.\n" +
                            "\tuseRounds=     Whether to use rounds (true) or turns (false). Defaults to false.\n" +
                            "\tstateFeatures  The name of a class that implements IStateFeatureVector.\n" +
                            "\tactionFeatures The name of a class that implements IActionFeatureVector.\n" +
                            "\tvalueSS=       File that contains the Search space to use with a value heuristic.\n" +
                            "\tactionSS=      File that contains the Search space to use with an action heuristic.\n" +
                            "\tprefix=        Name to use as output directory.\n" +
                            "\tdir=           The directory containing agent JSON files for learned heuristics and raw data\n" +
                            "\tgameParams=    (Optional) A JSON file from which the game parameters will be initialised.\n" +
                            "\tmatchups=      The number of games to play before the learning process is called.\n" +
                            "\titerations=    Stop after this number of learning iterations. Defaults to 100.\n" +
                            "\tfinalMatchups= The number of games to run in a final tournament between all agents. Defaults to 1000.\n" +
                            "\telite=         The number of agents to keep in the tournament. Defaults to iterations.\n"
            );
            return;
        }

        ExpertIteration pl = new ExpertIteration(args);

        pl.run();
    }

    public void run() {
        iter = 0;
        // load in the initial agent(s)
        agents.addAll(PlayerFactory.createPlayers(player));

        gatherData();

        learnFromNewData();

        //         tuneAgents();

        //          pruneAgents();
        iter++;

    }

    private void gatherData() {
        Map<RunArg, Object> config = RunArg.parseConfig(new String[]{}, Collections.singletonList(RunArg.Usage.RunGames));
        config.put(RunArg.matchups, matchups);
        config.put(RunArg.seed, System.currentTimeMillis());
        config.put(RunArg.byTeam, false);
        config.put(RunArg.mode, "exhaustive");
        config.put(RunArg.verbose, false);

        // we need to set the listener to record the required data for the Learner processes
        config.put(RunArg.listener, new ArrayList<String>());
        RoundRobinTournament tournament = new RoundRobinTournament(agents, gameToPlay, nPlayers, params, config);
        if (stateListener != null) {
            tournament.addListener(stateListener);
            String fileName = String.format("State_%s_%d.txt", prefix, iter);
            stateDataFilesByIteration[iter] = fileName;
            stateListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            stateListener.setOutputDirectory(dataDir);
        }
        if (actionListener != null) {
            tournament.addListener(actionListener);
            String fileName = String.format("Action_%s_%d.txt", prefix, iter);
            actionDataFilesByIteration[iter] = fileName;
            actionListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            actionListener.setOutputDirectory(dataDir);
        }
    }

    private void learnFromNewData() {
        // for the moment we will just supply the most recent file
        if (stateLearner != null) {
            Object thing = stateLearner.learnFrom(stateDataFilesByIteration[iter]);
            if (thing instanceof IToJSON toJSON) {
                // we need to write the learned heuristic to a file
                String fileName = prefix + "_ValueHeuristic_" + String.format("%2d", iter) + ".json";
                JSONUtils.writeJSON(toJSON.toJSON(), fileName);
            }
        }
        if (actionLearner != null) {
            Object thing = actionLearner.learnFrom(actionDataFilesByIteration[iter]);
            if (thing instanceof IToJSON toJSON) {
                // we need to write the learned heuristic to a file
                String fileName = prefix + "_ActionHeuristic_" + String.format("%2d", iter) + ".json";
                JSONUtils.writeJSON(toJSON.toJSON(), fileName);
            }
        }

    }
}
