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
import players.mcts.MCTSExpertIterationListener;
import players.mcts.MCTSPlayer;
import utilities.Pair;

import java.io.File;
import java.util.*;
import java.util.stream.IntStream;

import static evaluation.RunArg.parseConfig;
import static utilities.JSONUtils.loadClass;

public class ExpertIteration {

    GameType gameToPlay;
    String dataDir, player;
    AbstractParameters params;
    List<AbstractPlayer> agents;
    String stateLearnerFile, actionLearnerFile;
    IStateFeatureVector stateFeatureVector;
    IActionFeatureVector actionFeatureVector;
    FeatureListener stateListener, actionListener;
    int nPlayers, matchups, iterations, iter, bicMultiplier, everyN;
    String[] stateDataFilesByIteration;
    String[] actionDataFilesByIteration;
    boolean useRounds, useStateInAction;
    String prefix = "EI";
    AbstractPlayer bestAgent = null;
    int consecutiveTournamentWins = 0;
    Map<RunArg, Object> config;
    Map<RunArg, Object> NTBEAConfig;
    Map<RunArg, Object> RGConfig;

    int[] valueSearchSettings;
    int[] actionSearchSettings;
    ITPSearchSpace<?> valueSearchSpace = null;
    ITPSearchSpace<?> actionSearchSpace = null;

    public ExpertIteration(String[] args) {

        config = parseConfig(args, Collections.singletonList(RunArg.Usage.ExpertIteration));
        NTBEAConfig = parseConfig(args, Collections.singletonList(RunArg.Usage.ParameterSearch), false);
        RGConfig = parseConfig(args, Collections.singletonList(RunArg.Usage.RunGames), false);
        nPlayers = (int) config.get(RunArg.nPlayers);
        matchups = (int) config.get(RunArg.matchups);
        iterations = (int) config.get(RunArg.iterations);
        useRounds = (boolean) config.get(RunArg.useRounds);
        useStateInAction = (boolean) config.get(RunArg.stateForAction);
        player = (String) config.get(RunArg.playerDirectory);
        dataDir = (String) config.get(RunArg.destDir);
        gameToPlay = GameType.valueOf((String) config.get(RunArg.game));
        bicMultiplier = (int) config.get(RunArg.bicMultiplier);
        everyN = (int) config.get(RunArg.everyN);

        params = AbstractParameters.createFromFile(gameToPlay, (String) config.get(RunArg.gameParams));

        actionDataFilesByIteration = new String[iterations];
        stateDataFilesByIteration = new String[iterations];

        if (!config.get(RunArg.stateLearner).equals("")) {
            String featureDefinition = (String) config.get(RunArg.stateFeatures);
            if (featureDefinition.isEmpty())
                throw new IllegalArgumentException("Must specify stateFeatures for a stateLearner");
            stateLearnerFile = (String) config.get(RunArg.stateLearner);
        }
        if (!config.get(RunArg.actionLearner).equals("")) {
            String featureDefinition = (String) config.get(RunArg.actionFeatures);
            if (featureDefinition.isEmpty())
                throw new IllegalArgumentException("Must specify actionFeatures for an actionLearner");
            actionLearnerFile = (String) config.get(RunArg.actionLearner);
        }
        if (!config.get(RunArg.stateFeatures).equals("")) {
            String featureDefinition = (String) config.get(RunArg.stateFeatures);
            stateFeatureVector = loadClass(featureDefinition);
        }
        if (!config.get(RunArg.actionFeatures).equals("")) {
            String featureDefinition = (String) config.get(RunArg.actionFeatures);
            actionFeatureVector = loadClass(featureDefinition);
        } else if (actionLearnerFile == null && stateLearnerFile == null) {
            throw new IllegalArgumentException("Must specify at least one learner");
        }
    }

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            RunArg.printHelp(RunArg.Usage.ExpertIteration);
            return;
        }

        ExpertIteration pl = new ExpertIteration(args);

        pl.run();
    }

    private int restartIteration() {
        // Automatically determine restart iteration by checking for existing ValueNTBEA and ActionNTBEA json files
        int restartAtIteration = 0;
        while (true) {
            boolean valueExists = false, actionExists = false;
            if (stateLearnerFile != null) {
                String valueFile = dataDir + File.separator + String.format("ValueNTBEA_%02d.json", restartAtIteration);
                valueExists = new File(valueFile).exists();
            }
            if (actionLearnerFile != null) {
                String actionFile = dataDir + File.separator + String.format("ActionNTBEA_%02d.json", restartAtIteration);
                actionExists = new File(actionFile).exists();
            }
            if ((stateLearnerFile != null && !valueExists) && (actionLearnerFile != null && !actionExists)) {
                break;
            }
            if (stateLearnerFile != null && !valueExists) break;
            if (actionLearnerFile != null && !actionExists) break;
            restartAtIteration++;
        }
        return restartAtIteration;
    }

    public void run() {
        iter = 0;
        boolean finished = false;
        // load in the initial agent(s)
        agents = new ArrayList<>(PlayerFactory.createPlayers(player));
        bestAgent = agents.get(0);

        int restartAtIteration = restartIteration();

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
                AbstractPlayer newPlayer = null;
                if (stateLearnerFile != null) {
                    // Full player json is in NTBEA output directory
                    String agentFileName = dataDir + File.separator + String.format("ValueNTBEA_%02d.json", previousIter);
                    newPlayer = PlayerFactory.createPlayer(agentFileName);

                }
                if (actionLearnerFile != null) {
                    String agentFileName = dataDir + File.separator + String.format("ActionNTBEA_%02d.json", previousIter);
                    newPlayer = PlayerFactory.createPlayer(agentFileName);
                }
                newPlayer.setName(String.format("NTBEA_%02d.json", previousIter));
                agents.add(newPlayer);
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
        RGConfig.put(RunArg.mode, "random");  // we are most interested in a wide range of data, so do not want to reuse random seeds
        RGConfig.put(RunArg.verbose, false);

        // we need to set the listener to record the required data for the Learner processes
        RGConfig.put(RunArg.listener, new ArrayList<String>());

        // and set the budget on the agents
        int budget = (int) RGConfig.get(RunArg.budget);
        if (budget > 0) {
            for (AbstractPlayer player : agents) {
                if (player instanceof IAnyTimePlayer anyTime)
                    anyTime.setBudget(budget);
            }
        }

        RoundRobinTournament tournament = new RoundRobinTournament(agents, gameToPlay, nPlayers, params, RGConfig);
        tournament.setResultsFile(dataDir + File.separator + String.format("TournamentResults_%s_%02d.txt", prefix, iter));
        if (stateLearnerFile != null) {
            stateListener = new StateFeatureListener(stateFeatureVector,
                    useRounds ? Event.GameEvent.ROUND_OVER : Event.GameEvent.TURN_OVER,
                    false, "dummy.txt");
            stateListener.setNth(everyN);
            String fileName = String.format("State_%s_%02d.txt", prefix, iter);
            stateListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            stateListener.setOutputDirectory(dataDir);

            tournament.addListener(stateListener);
            stateDataFilesByIteration[iter] = dataDir + File.separator + fileName;
        }
        if (actionLearnerFile != null) {
            String expert = (String) config.get(RunArg.expert);
            MCTSPlayer oracle = (MCTSPlayer) bestAgent.copy();
            // For the oracle we set a high budget, and tweak parameters to ensure some exploration
            oracle.setName("Oracle");
            oracle.setBudget(budget * 10);
            oracle.getParameters().setParameterValue("rolloutLength", 10);
            oracle.getParameters().setParameterValue("reuseTree", false);
            oracle.getParameters().setParameterValue("maxTreeDepth", 1000);
            if (((double) oracle.getParameters().getParameterValue("FPU")) < 1000.0)
                oracle.getParameters().setParameterValue("FPU", 1000.0);
            if (((double) oracle.getParameters().getParameterValue("K")) < 1.0)
                oracle.getParameters().setParameterValue("K", 1.0);
            actionListener = switch (expert) {
                case "BASE" -> new ActionFeatureListener(actionFeatureVector, stateFeatureVector,
                        Event.GameEvent.ACTION_CHOSEN,
                        true, "dummy.txt");
                case "MCTS" -> new MCTSExpertIterationListener(oracle, actionFeatureVector, stateFeatureVector,
                        100, 0,
                        "dummy.txt");
                default -> throw new IllegalArgumentException("Unexpected value for expert: " + expert);
            };
            actionListener.setNth(everyN);
            String fileName = String.format("Action_%s_%02d.txt", prefix, iter);
            actionListener.setLogger(new FileStatsLogger(fileName, "\t", false));
            actionListener.setOutputDirectory(dataDir);

            tournament.addListener(actionListener);
            actionDataFilesByIteration[iter] = dataDir + File.separator + fileName;
        }
        tournament.run();

        // Are we done?
        if (tournament.getWinner().toString().equals(bestAgent.toString())) {
            consecutiveTournamentWins++;
        } else {
            consecutiveTournamentWins = 0;
        }
        bestAgent = tournament.getWinner().copy();
        if (bestAgent instanceof IAnyTimePlayer anyTime) {
            anyTime.setBudget(budget); // make sure the budget is set on the best agent
        }
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
                    loadClass(stateLearnerFile),
                    bicMultiplier);
            stateHeuristic = (IStateHeuristic) learnFromData.learn();
        }
        if (actionLearnerFile != null) {
            String fileName = prefix + "_ActionHeuristic_" + String.format("%02d", iter) + ".json";
            LearnFromData learnFromData = new LearnFromData(
                    actionDataFilesByIteration[iter],
                    useStateInAction ? stateFeatureVector : null,
                    actionFeatureVector,
                    dataDir + File.separator + fileName,
                    loadClass(actionLearnerFile),
                    bicMultiplier);
            actionHeuristic = (IActionHeuristic) learnFromData.learn();
        }
        return Pair.of(stateHeuristic, actionHeuristic);
    }

    private void tuneAgents(IStateHeuristic stateHeuristic, IActionHeuristic actionHeuristic) {
        // we now consider the value heuristic search space, and run NTBEA over this
        NTBEAConfig.put(RunArg.opponent, "random"); // this is overridden by bestAgent later...but is mandatory
        NTBEAConfig.put(RunArg.repeats, 1);
        NTBEAConfig.put(RunArg.evalGames, 0);

        AbstractPlayer newTunedPlayer = null;
        if (!config.get(RunArg.valueSS).equals("")) {
            NTBEAConfig.put(RunArg.searchSpace, config.get(RunArg.valueSS));
            NTBEAConfig.put(RunArg.destDir, dataDir + File.separator + String.format("ValueNTBEA_%02d", iter));
            NTBEAParameters ntbeaParams = new NTBEAParameters(NTBEAConfig);

            NTBEA ntbea = new NTBEA(ntbeaParams, gameToPlay, nPlayers);
            ntbea.setOpponents(Collections.singletonList(bestAgent));
            ntbea.fixTunableParameter("heuristic", stateHeuristic);  // so this is used when tuning

            if (actionSearchSettings != null) {
                if (actionSearchSpace != null) {   // on first iteration we have results of action search
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
                }
                ntbea.fixTunableParameter("actionHeuristic", actionHeuristic);  // so this is used when tuning
                ntbea.fixTunableParameter("rolloutPolicyParams.actionHeuristic", actionHeuristic);
            }

            ntbeaParams.printSearchSpaceDetails();
            Pair<Object, int[]> results = ntbea.run();
            valueSearchSettings = results.b;
            newTunedPlayer = (AbstractPlayer) results.a;
            valueSearchSpace = (ITPSearchSpace<?>) ntbeaParams.searchSpace;
            valueSearchSpace.writeAgentJSON(valueSearchSettings, dataDir + File.separator + String.format("ValueNTBEA_%02d.json", iter));
        }
        if (!config.get(RunArg.actionSS).equals("")) {
            NTBEAConfig.put(RunArg.searchSpace, config.get(RunArg.actionSS));
            NTBEAConfig.put(RunArg.destDir, dataDir + File.separator + String.format("ActionNTBEA_%02d", iter));
            NTBEAParameters ntbeaParams = new NTBEAParameters(NTBEAConfig);
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
            newTunedPlayer = (AbstractPlayer) results.a;
            actionSearchSpace.writeAgentJSON(actionSearchSettings, dataDir + File.separator + String.format("ActionNTBEA_%02d.json", iter));
        }
        String agentName = String.format("NTBEA_%02d.json", iter);
        newTunedPlayer.setName(agentName);
        agents.add(newTunedPlayer);
    }
}
