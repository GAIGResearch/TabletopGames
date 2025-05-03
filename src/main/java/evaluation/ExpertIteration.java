package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.*;
import evaluation.listeners.ActionFeatureListener;
import evaluation.listeners.FeatureListener;
import evaluation.listeners.StateFeatureListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.PlayerFactory;
import players.decorators.EpsilonRandom;
import players.learners.AbstractLearner;
import players.learners.LearnFromData;
import utilities.Pair;

import java.io.File;
import java.util.*;

import static utilities.JSONUtils.loadClass;
import static utilities.Utils.getArg;

public class ExpertIteration {

    String[] originalArgs;
    GameType gameToPlay;
    String dataDir, player, heuristic;
    AbstractParameters params;
    List<AbstractPlayer> agents;
    EpsilonRandom randomExplorer;
    AbstractLearner stateLearner, actionLearner;
    IStateFeatureVector stateFeatureVector;
    IActionFeatureVector actionFeatureVector;
    FeatureListener stateListener, actionListener;
    int nPlayers, matchups, iterations, iter, finalMatchups;
    double maxExplore;
    AbstractPlayer basePlayer;
    String[] stateDataFilesByIteration;
    String[] actionDataFilesByIteration;
    boolean useRounds, useStateInAction;
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
        actionDataFilesByIteration = new String[iterations];
        stateDataFilesByIteration = new String[iterations];

        useRounds = getArg(args, "useRounds", false);
        useStateInAction = getArg(args, "stateForAction", true);
        if (!getArg(args, "stateFeatures", "").isEmpty()) {
            stateFeatureVector = loadClass(getArg(args, "stateFeatures", ""));
            stateListener = new StateFeatureListener(stateFeatureVector,
                    useRounds ? Event.GameEvent.ROUND_OVER : Event.GameEvent.TURN_OVER,
                    false, "dummy.txt");
            String learnerDefinition = getArg(args, "stateLearner", "");
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
                            "\tstateForAction Whether to use the state features when learning the action heuristic. Defaults to true.\n" +
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
        agents = new ArrayList<>(PlayerFactory.createPlayers(player));

        // generate the training data
 //       gatherData();

        // learn the heuristics from the data
  //      Pair<IStateHeuristic, IActionHeuristic> learnedHeuristics = learnFromNewData();

        IStateHeuristic stateHeuristic = loadClass(dataDir + File.separator + prefix + "_ValueHeuristic_" + String.format("%2d", iter) + ".json");
        IActionHeuristic actionHeuristic = loadClass(dataDir + File.separator + prefix + "_ActionHeuristic_" + String.format("%2d", iter) + ".json");
  //      IStateHeuristic stateHeuristic = learnedHeuristics.a;
  //      IActionHeuristic actionHeuristic = learnedHeuristics.b;

        tuneAgents(stateHeuristic, actionHeuristic);

        //          pruneAgents();
        iter++;

    }

    private void gatherData() {
        Map<RunArg, Object> config = RunArg.parseConfig(new String[]{}, Collections.singletonList(RunArg.Usage.RunGames));
        config.put(RunArg.matchups, matchups);
        config.put(RunArg.seed, System.currentTimeMillis());
        config.put(RunArg.byTeam, false);
        config.put(RunArg.mode, "random");
        config.put(RunArg.verbose, false);

        // we need to set the listener to record the required data for the Learner processes
        config.put(RunArg.listener, new ArrayList<String>());
        RoundRobinTournament tournament = new RoundRobinTournament(agents, gameToPlay, nPlayers, params, config);
        if (stateListener != null) {
            tournament.addListener(stateListener);
            String fileName = String.format("State_%s_%d.txt", prefix, iter);
            stateDataFilesByIteration[iter] = dataDir + File.separator + fileName;
            stateListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            stateListener.setOutputDirectory(dataDir);
        }
        if (actionListener != null) {
            tournament.addListener(actionListener);
            String fileName = String.format("Action_%s_%d.txt", prefix, iter);
            actionDataFilesByIteration[iter] = dataDir + File.separator + fileName;
            actionListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            actionListener.setOutputDirectory(dataDir);
        }
        tournament.run();
    }

    // Learn agents from the data collected in the previous iteration
    // and add to the list of agents
    private Pair<IStateHeuristic, IActionHeuristic> learnFromNewData() {
        // for the moment we will just supply the most recent file
        IStateHeuristic stateHeuristic = null;
        IActionHeuristic actionHeuristic = null;
        if (stateLearner != null) {
            String fileName = prefix + "_ValueHeuristic_" + String.format("%2d", iter) + ".json";
            LearnFromData learnFromData = new LearnFromData(stateDataFilesByIteration[iter], stateFeatureVector, null,
            dataDir + File.separator + fileName, stateLearner);
            stateHeuristic = (IStateHeuristic) learnFromData.learn();
        }
        if (actionLearner != null) {
            String fileName = prefix + "_ActionHeuristic_" + String.format("%2d", iter) + ".json";
            LearnFromData learnFromData = new LearnFromData(actionDataFilesByIteration[iter], useStateInAction ? stateFeatureVector : null, actionFeatureVector,
                    dataDir + File.separator + fileName, actionLearner);
            actionHeuristic = (IActionHeuristic) learnFromData.learn();
        }
        return Pair.of(stateHeuristic, actionHeuristic);

    }

    private void tuneAgents(IStateHeuristic stateHeuristic, IActionHeuristic actionHeuristic) {
        // we now consider the value heuristic search space, and run NTBEA over this
        Map<RunArg, Object> config = RunArg.parseConfig(originalArgs, Collections.singletonList(RunArg.Usage.ParameterSearch));
        config.put(RunArg.searchSpace, getArg(originalArgs, "valueSS", ""));
        config.put(RunArg.opponent, "random"); // TODO: Change this to be best agent from last iteration
        NTBEAParameters ntbeaParams = new NTBEAParameters(config);

        NTBEA ntbea = new NTBEA(ntbeaParams, gameToPlay, nPlayers);
        ntbea.addToSearchSpace("heuristic", stateHeuristic);  // so this is used when tuning

        ntbeaParams.printSearchSpaceDetails();
        Pair<Object, int[]> results = ntbea.run();
        AbstractPlayer bestPlayer = (AbstractPlayer) results.a;
        bestPlayer.setName("ValueNTBEA_" + String.format("%2d", iter));

    }
}
