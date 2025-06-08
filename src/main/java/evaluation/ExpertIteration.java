package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.*;
import evaluation.listeners.ActionFeatureListener;
import evaluation.listeners.FeatureListener;
import evaluation.listeners.StateFeatureListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import evaluation.optimisation.ITPSearchSpace;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.IAnyTimePlayer;
import players.PlayerFactory;
import players.learners.LearnFromData;
import utilities.Pair;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static utilities.JSONUtils.loadClass;
import static utilities.Utils.getArg;

public class ExpertIteration {

    String[] originalArgs;
    GameType gameToPlay;
    String dataDir, player;
    AbstractParameters params;
    List<AbstractPlayer> agents;
    String stateLearnerFile, actionLearnerFile;
    IStateFeatureVector stateFeatureVector;
    IActionFeatureVector actionFeatureVector;
    FeatureListener stateListener, actionListener;
    int nPlayers, matchups, iterations, iter;
    String[] stateDataFilesByIteration;
    String[] actionDataFilesByIteration;
    boolean useRounds, useStateInAction;
    String prefix;
    boolean verbose;
    AbstractPlayer bestAgent = null;
    int consecutiveTournamentWins = 0;

    int[] valueSearchSettings;
    int[] actionSearchSettings;
    ITPSearchSpace<?> valueSearchSpace = null;
    ITPSearchSpace<?> actionSearchSpace = null;
    int restartAtIteration = 0; // if we want to restart the process at a later iteration

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
        iterations = getArg(args, "iterations", 100);
        verbose = getArg(args, "verbose", false);
        actionDataFilesByIteration = new String[iterations];
        stateDataFilesByIteration = new String[iterations];
        restartAtIteration = getArg(args, "restartAtIteration", 0);

        useRounds = getArg(args, "useRounds", false);
        useStateInAction = getArg(args, "stateForAction", true);
        if (!getArg(args, "stateLearner", "").isEmpty()) {
            String featureDefinition = getArg(args, "stateFeatures", "");
            if (featureDefinition.isEmpty())
                throw new IllegalArgumentException("Must specify stateFeatures for a stateLearner");
            stateLearnerFile = getArg(args, "stateLearner", "");
        }
        if (!getArg(args, "actionFeatures", "").isEmpty()) {
            String featureDefinition = getArg(args, "actionFeatures", "");
            if (featureDefinition.isEmpty())
                throw new IllegalArgumentException("Must specify actionFeatures for an actionLearner");
            actionLearnerFile = getArg(args, "actionLearner", "");
        }
        if (!getArg(args, "stateFeatures", "").isEmpty()) {
            stateFeatureVector = loadClass(getArg(args, "stateFeatures", ""));
        }
        if (!getArg(args, "actionFeatures", "").isEmpty())
            actionFeatureVector = loadClass(getArg(args, "actionFeatures", ""));
        else if (actionLearnerFile == null && stateLearnerFile == null) {
            throw new IllegalArgumentException("Must specify at least one learner");
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
                            "\tmatchups=      The number of games to play to gather data before the learning process is called.\n" +
                            "\titerations=    Stop after this number of learning iterations. Defaults to 100.\n");
            return;
        }

        ExpertIteration pl = new ExpertIteration(args);

        pl.run();
    }

    public void run() {
        iter = 0;
        boolean finished = false;
        // load in the initial agent(s)
        agents = new ArrayList<>(PlayerFactory.createPlayers(player));
        bestAgent = agents.get(0);

        if (restartAtIteration > 0) {
            // we are restarting the process, so we need to load the data files from the previous iteration
            iter = restartAtIteration;
            if (stateLearnerFile != null) {
                stateDataFilesByIteration[iter - 1] = dataDir + File.separator + String.format("State_%s_%02d.txt", prefix, iter - 1);
            }
            if (actionLearnerFile != null) {
                actionDataFilesByIteration[iter - 1] = dataDir + File.separator + String.format("Action_%s_%02d.txt", prefix, iter - 1);
            }

            // then load in the agents from the previous iterations
            for (int previousIter = 0; previousIter < restartAtIteration; previousIter++) {
                if (stateLearnerFile != null) {
                    // Full player json is in NTBEA output directory
                    String agentFileName = dataDir + File.separator + String.format("ValueNTBEA_%02d.json", previousIter);
                    AbstractPlayer newPlayer = PlayerFactory.createPlayer(agentFileName);
                    newPlayer.setName(String.format("ValueNTBEA_%02d.json", previousIter));
                    agents.add(newPlayer);
                }
                if (actionLearnerFile != null) {
                    String agentFileName = dataDir + File.separator + String.format("ActionNTBEA_%02d.json", previousIter);
                    AbstractPlayer newPlayer = PlayerFactory.createPlayer(agentFileName);
                    newPlayer.setName(String.format("ActionNTBEA_%02d.json", previousIter));
                    agents.add(newPlayer);
                }
            }
        }

        do {
            // learn the heuristics from the data
            finished = gatherDataAndCheckConvergence();
            //  stateDataFilesByIteration[0] = dataDir + File.separator + String.format("State_%s_%02d.txt", prefix, 0);
            //   actionDataFilesByIteration[0] = dataDir + File.separator + String.format("Action_%s_%02d.txt", prefix, 0);

            if (finished)
                break; // we are done, so we don't need to learn heuristics

            Pair<IStateHeuristic, IActionHeuristic> learnedHeuristics = learnFromNewData();

            IActionHeuristic actionHeuristic = learnedHeuristics.b;
            IStateHeuristic stateHeuristic = learnedHeuristics.a;

            tuneAgents(stateHeuristic, actionHeuristic);

            iter++;
        } while (true);
    }

    // A tournament of all current agents to gather data for the next training run
    // any very poorly performing agents are removed from the list (dominated by all other agents)
    // This also checks for convergence; meaning that the best agent has not changed for 3 iterations
    private boolean gatherDataAndCheckConvergence() {
        Map<RunArg, Object> config = RunArg.parseConfig(new String[]{}, Collections.singletonList(RunArg.Usage.RunGames), false);
        config.put(RunArg.matchups, matchups);
        config.put(RunArg.seed, System.currentTimeMillis());
        config.put(RunArg.byTeam, false);
        config.put(RunArg.mode, "random");  // we are most interested in a wide range of data, so do not want to reuse random seeds
        config.put(RunArg.verbose, false);
        config.put(RunArg.destDir, dataDir);

        // we need to set the listener to record the required data for the Learner processes
        config.put(RunArg.listener, new ArrayList<String>());

        // and set the budget on the agents
        int budget = getArg(originalArgs, "budget", 0);
        if (budget > 0) {
            for (AbstractPlayer player : agents) {
                if (player instanceof IAnyTimePlayer anyTime)
                    anyTime.setBudget(budget);
            }
        }

        RoundRobinTournament tournament = new RoundRobinTournament(agents, gameToPlay, nPlayers, params, config);
        tournament.setResultsFile(dataDir + File.separator + String.format("TournamentResults_%s_%02d.txt", prefix, iter));
        if (stateLearnerFile != null) {
            stateListener = new StateFeatureListener(stateFeatureVector,
                    useRounds ? Event.GameEvent.ROUND_OVER : Event.GameEvent.TURN_OVER,
                    false, "dummy.txt");
            stateListener.setNth(13);
            String fileName = String.format("State_%s_%02d.txt", prefix, iter);
            stateListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            stateListener.setOutputDirectory(dataDir);

            tournament.addListener(stateListener);
            stateDataFilesByIteration[iter] = dataDir + File.separator + fileName;
        }
        if (actionLearnerFile != null) {
            actionListener = new ActionFeatureListener(actionFeatureVector, stateFeatureVector,
                    Event.GameEvent.ACTION_CHOSEN,
                    true, "dummy.txt");
            actionListener.setNth(17);
            String fileName = String.format("Action_%s_%02d.txt", prefix, iter);
            actionListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            actionListener.setOutputDirectory(dataDir);

            tournament.addListener(actionListener);
            actionDataFilesByIteration[iter] = dataDir + File.separator + fileName;
        }
        tournament.run();

        // Are we done?
        if (tournament.getWinner().equals(bestAgent)) {
            consecutiveTournamentWins++;
        } else {
            consecutiveTournamentWins = 0;
        }
        bestAgent = tournament.getWinner();
        System.out.println("Best agent is " + bestAgent);

        if (agents.size() > nPlayers * 2) {
            // We then remove additional agents to get within 2 x nPlayers
            int toRemove = agents.size() - 2 * nPlayers;
            System.out.println("Removing " + toRemove + " additional agents to get within 2 x nPlayers");
            // we remove the worst performing agents
            List<Integer> sortedAgents = IntStream.range(0, agents.size())
                    .boxed()
                    .sorted(Comparator.comparingDouble(tournament::getWinRateAlphaRank))
                    .toList();
            // This sorts them in ascending order, so the first ones are the worst performing
            List<AbstractPlayer> toRemoveAgents = sortedAgents.stream()
                    .limit(toRemove)
                    .map(agents::get)
                    .peek(a -> System.out.println("Removing agent " + a))
                    .toList();
            agents.removeAll(toRemoveAgents);
        }
        if (consecutiveTournamentWins >= 3) {
            System.out.println("Converged after " + iter + " iterations");
            return true;
        }
        return false;

    }

    // Learn agents from the data collected in the previous iteration
    // and add to the list of agents
    private Pair<IStateHeuristic, IActionHeuristic> learnFromNewData() {
        // for the moment we will just supply the most recent file
        IStateHeuristic stateHeuristic = null;
        IActionHeuristic actionHeuristic = null;
        if (stateLearnerFile != null) {
            String fileName = prefix + "_ValueHeuristic_" + String.format("%02d", iter) + ".json";
            LearnFromData learnFromData = new LearnFromData(
                    stateDataFilesByIteration[iter],
                    stateFeatureVector,
                    null,
                    dataDir + File.separator + fileName,
                    loadClass(stateLearnerFile));
            stateHeuristic = (IStateHeuristic) learnFromData.learn();
        }
        if (actionLearnerFile != null) {
            String fileName = prefix + "_ActionHeuristic_" + String.format("%02d", iter) + ".json";
            LearnFromData learnFromData = new LearnFromData(
                    actionDataFilesByIteration[iter],
                    useStateInAction ? stateFeatureVector : null,
                    actionFeatureVector,
                    dataDir + File.separator + fileName,
                    loadClass(actionLearnerFile));
            actionHeuristic = (IActionHeuristic) learnFromData.learn();
        }
        return Pair.of(stateHeuristic, actionHeuristic);
    }

    private void tuneAgents(IStateHeuristic stateHeuristic, IActionHeuristic actionHeuristic) {
        // we now consider the value heuristic search space, and run NTBEA over this
        Map<RunArg, Object> config = RunArg.parseConfig(originalArgs, Collections.singletonList(RunArg.Usage.ParameterSearch), false);

        if (!getArg(originalArgs, "valueSS", "").isEmpty()) {
            config.put(RunArg.searchSpace, getArg(originalArgs, "valueSS", ""));
            config.put(RunArg.opponent, "random"); // this is overridden by bestAgent later...but is mandatory
            config.put(RunArg.destDir, dataDir + File.separator + String.format("ValueNTBEA_%02d", iter));
            NTBEAParameters ntbeaParams = new NTBEAParameters(config);

            NTBEA ntbea = new NTBEA(ntbeaParams, gameToPlay, nPlayers);
            ntbea.setOpponents(Collections.singletonList(bestAgent));
            ntbea.fixTunableParameter("heuristic", stateHeuristic);  // so this is used when tuning

            if (actionSearchSettings != null && actionSearchSpace != null) {
                // we can use the action search settings to initialise the value search settings
                List<String> valueNames = valueSearchSpace.getDimensions();
                for (int i = 0; i < actionSearchSettings.length; i++) {
                    if (!valueNames.contains(actionSearchSpace.name(i))) {
                        // usually we will have different parameters in the two searches, but if there is overlap we
                        // 'forget' the previous value
                        // otherwise we fix the non-optimised settings to the action search settings
                        ntbea.fixTunableParameter(actionSearchSpace.name(i), actionSearchSpace.value(i, actionSearchSettings[i]));
                    }
                }
                ntbea.fixTunableParameter("actionHeuristic", actionHeuristic);  // so this is used when tuning
                ntbea.fixTunableParameter("rolloutPolicyParams.actionHeuristic", actionHeuristic);
            }

            ntbeaParams.printSearchSpaceDetails();
            Pair<Object, int[]> results = ntbea.run();
            valueSearchSettings = results.b;
            AbstractPlayer bestPlayer = (AbstractPlayer) results.a;
            String agentName = String.format("ValueNTBEA_%02d.json", iter);
            bestPlayer.setName(agentName);
            valueSearchSpace = (ITPSearchSpace<?>) ntbeaParams.searchSpace;
            valueSearchSpace.writeAgentJSON(valueSearchSettings, dataDir + File.separator + agentName);
            agents.add(bestPlayer);
        }
        if (!getArg(originalArgs, "actionSS", "").isEmpty()) {

            config.put(RunArg.searchSpace, getArg(originalArgs, "actionSS", ""));
            config.put(RunArg.opponent, "random");
            config.put(RunArg.destDir, dataDir + File.separator + String.format("ActionNTBEA_%02d", iter));
            NTBEAParameters ntbeaParams = new NTBEAParameters(config);
            actionSearchSpace = (ITPSearchSpace<?>) ntbeaParams.searchSpace;

            NTBEA ntbea = new NTBEA(ntbeaParams, gameToPlay, nPlayers);
            ntbea.setOpponents(Collections.singletonList(bestAgent));
            ntbea.fixTunableParameter("actionHeuristic", actionHeuristic);  // so this is used when tuning
            ntbea.fixTunableParameter("rolloutPolicyParams.actionHeuristic", actionHeuristic);  // TODO: check if this is a parameter

            if (valueSearchSettings != null && valueSearchSpace != null) {
                // we can use the value search settings to initialise the action search settings
                List<String> actionNames = actionSearchSpace.getDimensions();
                for (int i = 0; i < valueSearchSettings.length; i++) {
                    if (!actionNames.contains(valueSearchSpace.name(i))) {
                        // usually we will have different parameters in the two searches, but if there is overlap we
                        // 'forget' the previous value
                        // otherwise we fix the non-optimised settings to the value search settings
                        ntbea.fixTunableParameter(valueSearchSpace.name(i), valueSearchSpace.value(i, valueSearchSettings[i]));
                    }
                }

                // and also make sure we include the state heuristic in the action search
                ntbea.fixTunableParameter("heuristic", stateHeuristic);
            }

            ntbeaParams.printSearchSpaceDetails();
            Pair<Object, int[]> results = ntbea.run();
            actionSearchSettings = results.b;
            AbstractPlayer bestPlayer = (AbstractPlayer) results.a;
            String agentName = String.format("ActionNTBEA_%02d.json", iter);
            bestPlayer.setName(agentName);
            actionSearchSpace.writeAgentJSON(actionSearchSettings, dataDir + File.separator + agentName);
            agents.add(bestPlayer);
        }

    }
}
