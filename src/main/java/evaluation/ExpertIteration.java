package evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.*;
import evaluation.listeners.ActionFeatureListener;
import evaluation.listeners.FeatureListener;
import evaluation.listeners.RolloutStateFeatureListener;
import evaluation.listeners.StateFeatureListener;
import evaluation.loggers.FileStatsLogger;
import evaluation.metrics.Event;
import evaluation.optimisation.ITPSearchSpace;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.tournaments.*;
import games.GameType;
import org.apache.commons.io.FileUtils;
import org.apache.spark.sql.catalyst.types.PhysicalArrayType;
import players.IAnyTimePlayer;
import players.PlayerFactory;
import players.learners.LearnFromData;
import players.mcts.MCTSExpertIterationListener;
import players.mcts.MCTSPlayer;
import utilities.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static evaluation.RunArg.parseConfig;
import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingInt;
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
    int nPlayers, matchups, iterations, iter, bicMultiplier, bicTimer, expertTime, maxRecords;
    double stateSampleRate, actionSampleRate;
    String[] stateDataFilesByIteration;
    String[] actionDataFilesByIteration;
    int[] stateRowsPerIteration, actionRowsPerIteration;
    boolean useRounds, useStateInAction;
    String prefix = "EI";
    AbstractPlayer bestAgent = null;
    String originalOpponentName;
    Map<String, Integer> tournamentWinsByAgent = new HashMap<>();
    int consecutiveWins = 0;
    Map<RunArg, Object> config;
    Map<RunArg, Object> NTBEAConfig;
    Map<RunArg, Object> RGConfig;

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    TournamentResults runningTournamentResults = new TournamentResults();

    int[] valueSearchSettings;
    int[] actionSearchSettings;
    ITPSearchSpace<?> valueSearchSpace = null;
    ITPSearchSpace<?> actionSearchSpace = null;

    public enum ValueTarget {Base, MCTS, Rollout, None}

    public enum ActionTarget {Base, MCTS, None}

    public enum TrainingMode {Batch, Exponential}

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
        bicTimer = (int) config.get(RunArg.bicTimer);
        stateSampleRate = (double) config.get(RunArg.sampleRate);
        actionSampleRate = (double) config.get(RunArg.sampleRate);
        expertTime = (int) config.get(RunArg.expertTime);
        maxRecords = (int) config.get(RunArg.maxRecords);

        params = AbstractParameters.createFromFile(gameToPlay, (String) config.get(RunArg.gameParams));

        int totalExpertIterations = (int) config.get(RunArg.expertIterations);
        actionRowsPerIteration = new int[totalExpertIterations];
        stateRowsPerIteration = new int[totalExpertIterations];
        stateDataFilesByIteration = new String[totalExpertIterations];
        actionDataFilesByIteration = new String[totalExpertIterations];

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

    public void run() {
        iter = 0;
        boolean finished = false;
        // load in the initial agent(s)
        agents = new ArrayList<>(PlayerFactory.createPlayers(player));
        bestAgent = agents.getFirst();
        originalOpponentName = bestAgent.toString();

        IActionHeuristic currentActionHeuristic = null;

        // Check to see if we are re-starting a previously aborted run
        Pair<Integer, Integer> completedIterations = checkCompletedIterations();
        int restartAtIteration = completedIterations.a;
        boolean restartWithTuning = completedIterations.b > 0 && !Objects.equals(completedIterations.a, completedIterations.b);
        iter = restartAtIteration;
        if (completedIterations.a > 0 && completedIterations.b > 0) {
            System.out.printf("Restarting from iteration %d (with tuning: %b)%n", restartAtIteration, restartWithTuning);
        }

        try {
            if ((new File(dataDir + File.separator + "RunningTournamentResults.json")).exists()) {
                TournamentResults.TournamentResultsDTO dto = mapper.readValue(new File(dataDir + File.separator + "RunningTournamentResults.json"), TournamentResults.TournamentResultsDTO.class);
                runningTournamentResults = TournamentResults.getTournamentResults(dto);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        if (restartWithTuning) {
            int iterationRef = config.get(RunArg.expertTrainingMode) == TrainingMode.Exponential ? 0 : iter;
            if (stateLearnerFile != null)
                stateDataFilesByIteration[iterationRef] = dataDir + File.separator + String.format("State_%s_%02d.txt", prefix, iterationRef);
            if (actionLearnerFile != null)
                actionDataFilesByIteration[iterationRef] = dataDir + File.separator + String.format("Action_%s_%02d.txt", prefix, iterationRef);
        }
        if (restartAtIteration > 0) {
            int iterationRef = config.get(RunArg.expertTrainingMode) == TrainingMode.Exponential ? 0 : iter - 1;
            // we are restarting the process, so we need to load the data files from the previous iteration
            if (stateLearnerFile != null) {
                stateDataFilesByIteration[iterationRef] = dataDir + File.separator + String.format("State_%s_%02d.txt", prefix, iterationRef);
            }
            if (actionLearnerFile != null) {
                actionDataFilesByIteration[iterationRef] = dataDir + File.separator + String.format("Action_%s_%02d.txt", prefix, iterationRef);
            }

            // then load in the agents from the previous iterations
            // we first load in all agents
            for (int previousIter = 0; previousIter < restartAtIteration; previousIter++) {
                String playerName = String.format("NTBEA_%02d.json", previousIter);
                AbstractPlayer newPlayer = null;
                boolean hasValueSS = !config.get(RunArg.valueSS).equals("");
                boolean hasActionSS = !config.get(RunArg.actionSS).equals("");
                if (hasValueSS) {
                    // Full player json is in NTBEA output directory
                    String agentFileName = dataDir + File.separator + String.format("ValueNTBEA_%02d.json", previousIter);
                    newPlayer = PlayerFactory.createPlayer(agentFileName);

                }
                if (hasActionSS) {
                    String agentFileName = dataDir + File.separator + String.format("ActionNTBEA_%02d.json", previousIter);
                    newPlayer = PlayerFactory.createPlayer(agentFileName);
                }
                if (newPlayer == null) {
                    throw new IllegalStateException("Could not load agent from previous iteration");
                }
                newPlayer.setName(playerName);
                agents.add(newPlayer);
                runningTournamentResults.registerAgent(newPlayer);
            }

            // the above code has loaded all agents...we now cut this down to just the ones that were in the
            // running at the point from which we are reloading
            for (AbstractPlayer agent : agents) {
                if (runningTournamentResults.getPlayerResults(agent.toString()).isEmpty()) {
                    // has already been removed
                    runningTournamentResults.filterPlayer(agent.toString());
                    agents.remove(agent);
                }
            }
            // then work out who the current best agent is
            WinRateAnalysis winRateAnalysis = new WinRateAnalysis();
            String bestAgentName = winRateAnalysis.getRanking(runningTournamentResults).firstEntry().getKey();
            bestAgent = agents.stream().filter(a -> a.toString().equals(bestAgentName)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Best agent from previous run not found among loaded agents"));

        }

        do {
            long iterationStartTime = System.currentTimeMillis();
            // learn the heuristics from the data
            // it is possible we skip this if restarting (as the data is still there from the previous run)
            if (restartWithTuning) {
                restartWithTuning = false;
            } else {
                finished = gatherDataAndCheckConvergence();
            }

            long dataGatheringTime = System.currentTimeMillis() - iterationStartTime;
            if (!finished) {
                Pair<IStateHeuristic, IActionHeuristic> learnedHeuristics = learnFromNewData();
                long learningTime = System.currentTimeMillis() - iterationStartTime - dataGatheringTime;

                IActionHeuristic newActionHeuristic = learnedHeuristics.b;
                IStateHeuristic newStateHeuristic = learnedHeuristics.a;

                tuneAgents(newStateHeuristic, newActionHeuristic, currentActionHeuristic);
                long tuningTime = System.currentTimeMillis() - iterationStartTime - dataGatheringTime - learningTime;

                currentActionHeuristic = newActionHeuristic;
                Pair<Long, Long> totalTime = calculateHoursAndMinutes(System.currentTimeMillis() - iterationStartTime);
                Pair<Long, Long> dataTime = calculateHoursAndMinutes(dataGatheringTime);
                Pair<Long, Long> learnTime = calculateHoursAndMinutes(learningTime);
                Pair<Long, Long> tuneTime = calculateHoursAndMinutes(tuningTime);
                System.out.printf(
                        "Iteration %d completed in %d h %2d m (data: %d h %2d m, learn: %d h %2d m, tune: %d h %2d m)%n",
                        iter, totalTime.a, totalTime.b,
                        dataTime.a, dataTime.b,
                        learnTime.a, learnTime.b,
                        tuneTime.a, tuneTime.b
                );

                // record the amount of data gathered for this iteration
                stateRowsPerIteration[iter] = getTotalDataSize("state");
                if (iter > 0) stateRowsPerIteration[iter] -= stateRowsPerIteration[iter - 1];
                actionRowsPerIteration[iter] = getTotalDataSize("action");
                if (iter > 0) actionRowsPerIteration[iter] -= actionRowsPerIteration[iter - 1];

                // we can now adjust some parameters based on empirical data (data gathered per iteration, and time taken)
                // generally speaking we want each iteration to generate about 10% of the total data
                // we therefore calculate sample rates for each of value and actions to achieve this
                stateSampleRate = Math.min(0.05, stateSampleRate * maxRecords / 10.0 / (double) stateRowsPerIteration[iter]);
                // we do not want to sample more than about 5% of state to avoid over-correlation between samples (not such an issue for actions)
                actionSampleRate = Math.min(1.0, actionSampleRate * maxRecords / 10.0 / (double) actionRowsPerIteration[iter]);
                System.out.printf("State records gathered: %d, Action records gathered: %d%n", stateRowsPerIteration[iter], actionRowsPerIteration[iter]);
                System.out.printf("State sample rate: %.3f, Action sample rate: %.3f\n", stateSampleRate, actionSampleRate);

                //TODO: We also don't want the learning process to take up more than about 1/3rd of the total time. So we adjust
                // the limit on maxRecords if needed
                iter++;

            }
        } while (!finished);

        // Now we want to write out the final winning agent, and also all other agents still in the competition
        // as these may have non-transitive behaviours

        // For each of them we want to give them a name of the format "FinalAgent_X_YY.json"
        // where X is a unique identifier for the agent (e.g. its rank in the final tournament)
        // and YY is the alpha rank of the agent in the final tournament.

        ParetoAnalysis paretoAnalysis = new ParetoAnalysis();
        Map<String, Pair<Double, Double>> paretoRankings = paretoAnalysis.getRanking(runningTournamentResults);

        List<String> firstParetoFront = paretoRankings.entrySet().stream()
                .filter(e -> e.getValue().a == 1.0)
                .map(Map.Entry::getKey)
                .toList();

        AlphaRankAnalysis alphaRankAnalysis = new AlphaRankAnalysis(false);
        Map<String, Pair<Double, Double>> alphaRankings = alphaRankAnalysis.getRanking(runningTournamentResults);
        agents.sort(comparingDouble(a -> -alphaRankings.get(a.toString()).a)); // then sort by alpha rank00
        for (int i = 0; i < agents.size(); i++) {
            if (firstParetoFront.contains(agents.get(i).toString())) {
                // only save those agents on the Pareto Front
                AbstractPlayer agent = agents.get(i);
                String newFileName = String.format("FinalAgent_R%02d_A%2d.json", i + 1, Math.round(alphaRankings.get(agent.toString()).a * 100.0));
                String originalFileName;
                if (agents.get(i).toString().equals(originalOpponentName))
                    originalFileName = player;
                else {
                    // format is XXX_03.json"
                    int originalIteration = Integer.parseInt(agent.toString().split("_")[1].replaceAll("\\D+", ""));
                    originalFileName = String.format("%sNTBEA_%02d.json", config.get(RunArg.valueSS).equals("") ? "Action" : "Value", originalIteration);
                }
                try {
                    // we now copy the file for the agent
                    File oldFile = new File(dataDir + File.separator + originalFileName);
                    File newFile = new File(dataDir + File.separator + "FinalAgents" + File.separator + newFileName);
                    FileUtils.copyFile(oldFile, newFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    /**
     * Returns a pair of numbers.
     * The first is the number of completed iterations (through to generation of tuned agents)
     * The second is the number of partially completed iterations that generated full data, but not tuned agents
     * The second number will be one higher than the first if the previous iteration failed in the tuning phase.
     */
    private Pair<Integer, Integer> checkCompletedIterations() {
        // Automatically determine restart iteration by checking for existing ValueNTBEA and ActionNTBEA json files
        boolean hasValueSS = !config.get(RunArg.valueSS).equals("");
        boolean hasActionSS = !config.get(RunArg.actionSS).equals("");
        int completedIterations = 0;
        while (true) {
            boolean valueExists = false, actionExists = false;
            if (hasValueSS) {
                String valueFile = dataDir + File.separator + String.format("ValueNTBEA_%02d.json", completedIterations);
                valueExists = new File(valueFile).exists();
            }
            if (hasActionSS) {
                String actionFile = dataDir + File.separator + String.format("ActionNTBEA_%02d.json", completedIterations);
                actionExists = new File(actionFile).exists();
            }
            boolean agentsOKForState = !hasValueSS || valueExists;
            boolean agentsOKForAction = !hasActionSS || actionExists;
            if (!agentsOKForState || !agentsOKForAction) {
                // we now check to see if the data has been gathered for the next iteration
                int iterationRef = config.get(RunArg.expertTrainingMode) == TrainingMode.Exponential ? 0 : completedIterations;
                String stateDataFile = dataDir + File.separator + String.format("State_%s_%02d.txt", prefix, iterationRef);
                String actionDataFile = dataDir + File.separator + String.format("Action_%s_%02d.txt", prefix, iterationRef);
                boolean dataOKForState = stateLearnerFile == null || new File(stateDataFile).exists();
                boolean dataOKForAction = actionLearnerFile == null || new File(actionDataFile).exists();
                if (dataOKForState && dataOKForAction) {
                    return new Pair<>(completedIterations, completedIterations + 1);
                } else {
                    return new Pair<>(completedIterations, completedIterations);
                }
            }
            completedIterations++;
        }
    }


    /**
     * Converts milliseconds to a Pair of hours and minutes.
     *
     * @param millis Time in milliseconds.
     * @return Pair where a = hours, b = minutes.
     */
    private Pair<Long, Long> calculateHoursAndMinutes(long millis) {
        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis / (1000 * 60)) % 60;
        return Pair.of(hours, minutes);
    }

    // A tournament of all current agents to gather data for the next training run
    // any very poorly performing agents are removed from the list (dominated by all other agents)
    // This also checks for convergence; meaning that the best agent has not changed for 3 iterations
    private boolean gatherDataAndCheckConvergence() {
        Map<RunArg, Object> tournamentConfig = new HashMap<>(RGConfig);
        tournamentConfig.put(RunArg.verbose, false);

        // we need to set the listener to record the required data for the Learner processes
        tournamentConfig.put(RunArg.listener, new ArrayList<String>());

        // and set the budget on the agents
        int budget = (int) tournamentConfig.get(RunArg.budget);
        if (budget > 0) {
            for (AbstractPlayer player : agents) {
                if (player instanceof IAnyTimePlayer anyTime)
                    anyTime.setBudget(budget);
            }
        }
        // we want to run 2 tournaments. Firstly with a focusPlayer of the newly learned agent
        // then one with all agents - each using half the matchups budget.
        // Unless there is a single agent; or the best agent is still the first one. In this case, we run a single tournament with the full budget.
        if (!agents.isEmpty() && bestAgent != agents.getFirst()) {
            tournamentConfig.put(RunArg.mode, "onevsall");
            tournamentConfig.put(RunArg.matchups, (int) RGConfig.get(RunArg.matchups) * (nPlayers - 1) / nPlayers);
            List<AbstractPlayer> focusAtFront = new ArrayList<>(agents.reversed());
            // this will have the newly learned agent at the front
            runTournament(focusAtFront, tournamentConfig);
        } else {
            tournamentConfig.put(RunArg.matchups, 0); // indicate we have not used any of the budget
        }

        tournamentConfig.put(RunArg.mode, "random");
        // use remaining budget
        tournamentConfig.put(RunArg.matchups, (int) RGConfig.get(RunArg.matchups) - (int) tournamentConfig.get(RunArg.matchups));
        RoundRobinTournament tournament = runTournament(agents, tournamentConfig);

        AlphaRankAnalysis alphaRankAnalysis = new AlphaRankAnalysis(false);
        Map<String, Pair<Double, Double>> alphaRankings = alphaRankAnalysis.getRanking(tournament.getTournamentResults());
        List<String> agentsSortedByAlphaRank = alphaRankings.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().a, e1.getValue().a)) // sort descending by alpha rank
                .map(Map.Entry::getKey)
                .toList();
        String alphaRankWinner = agentsSortedByAlphaRank.isEmpty() ? tournament.getWinner().toString() : agentsSortedByAlphaRank.getFirst();

        AbstractPlayer winner = tournament.getTournamentResults().getAgent(alphaRankWinner);
        // Are we done?
        if (iter > 0) {
            tournamentWinsByAgent.merge(winner.toString(), 1, Integer::sum);
            if (winner.toString().equals(bestAgent.toString())) {
                consecutiveWins++;
            } else {
                consecutiveWins = 1; // reset the counter
            }
        }
        bestAgent = winner.copy();

        if (bestAgent instanceof IAnyTimePlayer anyTime) {
            anyTime.setBudget(budget); // make sure the budget is set on the best agent
        }
        System.out.println("Best agent is " + bestAgent);

        if (agents.size() > nPlayers * 2) {
            // We may remove additional agents to get within 2 x nPlayers
            int toRemove = agents.size() - 2 * nPlayers;
            System.out.println("Attempting to remove " + toRemove + " additional agents to get within 2 x nPlayers");
            // firstly find any agents that are dominated by all others (i.e. beaten by all other agents in head-to-head matchups)
            List<String> dominatedAgents = runningTournamentResults.getDominatedAgents();
            toRemove -= dominatedAgents.size();
            System.out.println("Removing " + dominatedAgents.size() + " strictly dominated agents");
            agents.removeIf(a -> dominatedAgents.contains(a.toString()));
            for (String removed : dominatedAgents) {
                System.out.println("\tRemoving " + removed);
                runningTournamentResults.filterPlayer(removed);
            }
            if (toRemove > 0) {
                // we now see if there are clusters of agents, and remove the worst agent from the largest cluster
                List<String> poorClusterPerformers = new ArrayList<>();
                double[] thresholds = new double[]{0.02, 0.05, 0.1, 0.2};
                for (double threshold : thresholds) {
                    Map<String, List<String>> clusters = alphaRankAnalysis.calculateClusters(runningTournamentResults, threshold);
                    Map<String, List<String>> clustersWithMoreThanOneMember = clusters.keySet().stream()
                            .filter(cName -> clusters.get(cName).size() > 1)
                            .collect(Collectors.toMap(c -> c, clusters::get));
                    if (clustersWithMoreThanOneMember.isEmpty()) {
                        System.out.printf("No clusters with a threshold of %.2f%n", threshold);
                        continue; // try next largest thresholds
                    }

                    System.out.printf("%d clusters found at threshold of %2f (%s)%n\t",
                            clustersWithMoreThanOneMember.size(), threshold,
                            clustersWithMoreThanOneMember.values().stream().map(List::toString).collect(Collectors.joining(", ")));
                    System.out.println();
                    // Now we find the poorest performer in each cluster
                    for (String clusterName : clustersWithMoreThanOneMember.keySet()) {
                        String poorestPerformer = "";
                        double performance = Double.POSITIVE_INFINITY;
                        for (String agent : clustersWithMoreThanOneMember.get(clusterName)) {
                            double p = alphaRankings.get(agent).a;
                            if (p < performance) {
                                performance = p;
                                poorestPerformer = agent;
                            }
                        }
                        poorClusterPerformers.add(poorestPerformer);
                    }
                    break;  // we stop once we've found the narrowest clusters
                }
                agents.removeIf(a -> poorClusterPerformers.contains(a.toString()));
                for (String removed : poorClusterPerformers) {
                    System.out.println("Removing " + removed + " as poorest performer in cluster");
                    runningTournamentResults.filterPlayer(removed);
                }
            }
        }
        // we end if any agent has won N consecutive tournaments
        if (consecutiveWins >= (int) config.get(RunArg.expertConvergence)) {
            System.out.println("Converged after " + iter + " iterations");
            return true;
        }
        if (iter >= (int) config.get(RunArg.expertIterations)) {
            System.out.println("Reached maximum iterations of " + iter);
            return true;
        }
        return false;
    }

    private int getTotalDataSize(String fileType) {
        // for all stateLearner files, open them and check their length in rows. Return the total
        String[] files = switch (fileType) {
            case "state" -> stateDataFilesByIteration;
            case "action" -> actionDataFilesByIteration;
            default -> throw new IllegalArgumentException("Invalid file type: " + fileType);
        };
        int totalDataSize = 0;
        for (String fileName : files) {
            if (fileName == null || fileName.isEmpty()) continue;
            try (BufferedReader reader = Files.newBufferedReader(Path.of(fileName))) {
                while (reader.readLine() != null) {
                    totalDataSize++;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return totalDataSize;
    }

    private RoundRobinTournament runTournament(List<AbstractPlayer> localAgents, Map<RunArg, Object> runGamesConfig) {

        ActionTarget actionTarget = (ActionTarget) config.get(RunArg.actionTarget);
        ValueTarget valueTarget = (ValueTarget) config.get(RunArg.valueTarget);
        if (valueTarget == ValueTarget.None) stateLearnerFile = null;
        if (actionTarget == ActionTarget.None) actionLearnerFile = null;
        boolean allDataAsOne = config.get(RunArg.expertTrainingMode) == TrainingMode.Exponential;
        RoundRobinTournament tournament = new RoundRobinTournament(localAgents, gameToPlay, nPlayers, params, runGamesConfig);
        tournament.setTournamentResults(runningTournamentResults);
        tournament.setResultsFile(dataDir + File.separator + String.format("TournamentResults_%s_%02d.txt", prefix, iter));
        if (stateLearnerFile != null) {
            stateListener = switch (valueTarget) {
                case ValueTarget.Base -> new StateFeatureListener(stateFeatureVector,
                        useRounds ? Event.GameEvent.ROUND_OVER : Event.GameEvent.TURN_OVER,
                        false);  // i.e. we use the actual game outcome as the Value target
                case ValueTarget.MCTS -> {
                    if (config.get(RunArg.actionTarget) != ActionTarget.MCTS)
                        throw new IllegalArgumentException("Cannot use a valueTarget of MCTS unless actionTarget is also MCTS");
                    yield null;  // covered by ActionListener (i.e. we use the MCTS estimate as the Value target)
                }
                case ValueTarget.Rollout -> {
                    AbstractPlayer[] players = new AbstractPlayer[nPlayers];
                    for (int i = 0; i < players.length; i++) {
                        players[i] = bestAgent.copy();
                        players[i].setPlayerID(i);
                    }
                    yield (new RolloutStateFeatureListener(
                            stateFeatureVector,
                            players,
                            gameToPlay.createForwardModel(params, nPlayers)))
                            .recordEndGameState(false);
                }
                default ->
                        throw new IllegalArgumentException("Unexpected value for expert: " + config.get(RunArg.valueTarget));
            };
            String fileName = String.format("State_%s_%02d.txt", prefix, allDataAsOne ? 0 : iter);
            stateDataFilesByIteration[allDataAsOne ? 0 : iter] = dataDir + File.separator + fileName;
            if (stateListener != null) {
                stateListener = stateListener
                        .setSampleRate(stateSampleRate)
                        .setLogger(new FileStatsLogger(fileName, "\t", allDataAsOne));
                stateListener.setOutputDirectory(dataDir);
                tournament.addListener(stateListener);
            }
        }
        if (actionLearnerFile != null) {

            actionListener = switch (config.get(RunArg.actionTarget)) {
                case ActionTarget.Base -> new ActionFeatureListener(actionFeatureVector, stateFeatureVector,
                        Event.GameEvent.ACTION_CHOSEN,
                        true);
                case ActionTarget.MCTS -> {
                    MCTSPlayer oracle = (bestAgent instanceof MCTSPlayer) ? (MCTSPlayer) bestAgent.copy() : ((MCTSPlayer) agents.getLast()).copy();
                    if (oracle == null) {
                        throw new IllegalArgumentException("Best agent must be an MCTSPlayer to use MCTS as action target");
                    }
                    // For the oracle we set a high budget, and tweak parameters to ensure some exploration
                    oracle.setName("Oracle");
                    oracle.setBudget((int) config.get(RunArg.budget) * expertTime);
                    oracle.getParameters().setParameterValue("reuseTree", false); // we only look at occasional actions
                    //       oracle.getParameters().setParameterValue("maxTreeDepth", 1000);
                    // then in this case we have to shackle the action and state sample rates
                    double commonRate = Math.min(actionSampleRate, stateSampleRate);
                    actionSampleRate = commonRate;
                    stateSampleRate = commonRate;
                    if (((double) oracle.getParameters().getParameterValue("FPU")) < 1000.0)
                        oracle.getParameters().setParameterValue("FPU", 1000.0);
                    if (((double) oracle.getParameters().getParameterValue("K")) < 1.0)
                        oracle.getParameters().setParameterValue("K", 1.0);
                    yield new MCTSExpertIterationListener(oracle, actionFeatureVector, stateFeatureVector,
                            100, 0, stateLearnerFile != null && stateListener == null);
                }
                // we record the MCTS stats for every action, plus the state features if we are not already recording them with a separate listener
                default ->
                        throw new IllegalArgumentException("Unexpected value for expert: " + config.get(RunArg.actionTarget));
            };
            String fileName = String.format("Action_%s_%02d.txt", prefix, allDataAsOne ? 0 : iter);
            actionListener = actionListener
                    .setLogger(new FileStatsLogger(fileName, "\t", allDataAsOne))
                    .setSampleRate(actionSampleRate);
            actionListener.setOutputDirectory(dataDir);

            tournament.addListener(actionListener);
            actionDataFilesByIteration[allDataAsOne ? 0 : iter] = dataDir + File.separator + fileName;
        }
        tournament.run();
        // then save running tournament results in case of a restart (and if there was more than one agent involved)
        if (localAgents.size() > 1) {
            try {
                // write the DTO so Jackson serializes a JSON-friendly structure
                mapper.writeValue(new File(dataDir + File.separator + "RunningTournamentResults.json"), runningTournamentResults.toDTO());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return tournament;
    }

    // Learn agents from the data collected in the previous iteration
    // and add to the list of agents
    private Pair<IStateHeuristic, IActionHeuristic> learnFromNewData() {
        // for the moment we will just supply the most recent file
        IStateHeuristic stateHeuristic = null;
        IActionHeuristic actionHeuristic = null;
        boolean allData = config.get(RunArg.expertTrainingMode) == TrainingMode.Exponential;
        if (stateLearnerFile != null) {
            String fileName = prefix + "_ValueHeuristic_" + String.format("%02d", iter) + ".json";
            LearnFromData learnFromData = new LearnFromData(
                    stateDataFilesByIteration[allData ? 0 : iter],
                    stateFeatureVector,
                    null,
                    dataDir + File.separator + fileName,
                    loadClass(stateLearnerFile),
                    bicMultiplier,
                    bicTimer);
            learnFromData.setMaxRecords(maxRecords);
            stateHeuristic = (IStateHeuristic) learnFromData.learn();
        }
        if (actionLearnerFile != null) {
            String fileName = prefix + "_ActionHeuristic_" + String.format("%02d", iter) + ".json";
            LearnFromData learnFromData = new LearnFromData(
                    actionDataFilesByIteration[allData ? 0 : iter],
                    useStateInAction ? stateFeatureVector : null,
                    actionFeatureVector,
                    dataDir + File.separator + fileName,
                    loadClass(actionLearnerFile),
                    bicMultiplier,
                    bicTimer);
            learnFromData.setMaxRecords(maxRecords);
            actionHeuristic = (IActionHeuristic) learnFromData.learn();
        }
        return Pair.of(stateHeuristic, actionHeuristic);
    }

    private void tuneAgents(IStateHeuristic stateHeuristic, IActionHeuristic actionHeuristic, IActionHeuristic oldActionHeuristic) {
        // we now consider the value heuristic search space, and run NTBEA over this
        NTBEAConfig.put(RunArg.opponent, "random"); // this is overridden by bestAgent later...but is mandatory
        NTBEAConfig.put(RunArg.repeats, 1);
        NTBEAConfig.put(RunArg.evalGames, 0);

        AbstractPlayer newTunedPlayer = null;
        if (!config.get(RunArg.valueSS).equals("")) { // we tune with a value search space
            NTBEAConfig.put(RunArg.searchSpace, config.get(RunArg.valueSS));
            NTBEAConfig.put(RunArg.destDir, dataDir + File.separator + String.format("ValueNTBEA_%02d", iter));
            NTBEAParameters ntbeaParams = new NTBEAParameters(NTBEAConfig);

            NTBEA ntbea = new NTBEA(ntbeaParams, gameToPlay, nPlayers);
            ntbea.setOpponents(Collections.singletonList(bestAgent));

            // fix to current action settings (if we have a two-phase tuning approach)
            if (actionSearchSettings != null) {
                if (actionSearchSpace != null) {   // on first iteration we have results of action search
                    // we can use the action search settings to initialise the value search settings
                    fixSSDimensions(ntbea, valueSearchSpace, actionSearchSettings, actionSearchSpace,
                            Set.of("heuristic", "actionHeuristic", "rolloutPolicyParams", "rolloutPolicyParams.actionHeuristic"));
                }
            }

            // also set currently learned heuristics
            ntbea.fixTunableParameter("heuristic", stateHeuristic);  // so this is used when tuning
            if (actionHeuristic != null) {
                ntbea.fixTunableParameter("actionHeuristic", actionHeuristic);
                ntbea.fixTunableParameter("rolloutPolicyParams.actionHeuristic", actionHeuristic);
            }

            ntbeaParams.printSearchSpaceDetails();
            Pair<Object, int[]> results = ntbea.run();
            valueSearchSettings = results.b;
            newTunedPlayer = (AbstractPlayer) results.a;
            valueSearchSpace = (ITPSearchSpace<?>) ntbeaParams.searchSpace;
            valueSearchSpace.writeAgentJSON(valueSearchSettings, dataDir + File.separator + String.format("ValueNTBEA_%02d.json", iter));
        }

        // then we run the second action phase (if set to do so)
        if (!config.get(RunArg.actionSS).equals("")) {
            NTBEAConfig.put(RunArg.searchSpace, config.get(RunArg.actionSS));
            NTBEAConfig.put(RunArg.destDir, dataDir + File.separator + String.format("ActionNTBEA_%02d", iter));
            NTBEAParameters ntbeaParams = new NTBEAParameters(NTBEAConfig);
            actionSearchSpace = (ITPSearchSpace<?>) ntbeaParams.searchSpace;

            NTBEA ntbea = new NTBEA(ntbeaParams, gameToPlay, nPlayers);
            ntbea.setOpponents(Collections.singletonList(bestAgent));

            if (valueSearchSettings != null && valueSearchSpace != null) {
                // we can use the value search settings to initialise the action search settings
                fixSSDimensions(ntbea, actionSearchSpace, valueSearchSettings, valueSearchSpace,
                        Set.of("heuristic", "actionHeuristic", "rolloutPolicyParams", "rolloutPolicyParams.actionHeuristic"));
            }

            // set current heuristics
            ntbea.fixTunableParameter("actionHeuristic", actionHeuristic);  // so this is used when tuning
            ntbea.fixTunableParameter("rolloutPolicyParams.actionHeuristic", actionHeuristic);
            if (stateHeuristic != null) {
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

    public static void fixSSDimensions(NTBEA ntbea,
                                       ITPSearchSpace<?> searchSpaceToFix,
                                       int[] searchSettings,
                                       ITPSearchSpace<?> referenceSearchSpace,
                                       Set<String> exclusions) {
        List<String> actionNames = searchSpaceToFix.getDimensions();
        for (int i = 0; i < searchSettings.length; i++) {
            if (!actionNames.contains(referenceSearchSpace.name(i))) {
                // usually we will have different parameters in the two searches, but if there is overlap we
                // 'forget' the previous value
                // otherwise we fix the non-optimised settings to the value search settings
                ntbea.fixTunableParameter(referenceSearchSpace.name(i), referenceSearchSpace.value(i, searchSettings[i]));
            }
        }

        // Safety check for non-tuned parameters that differ between the two search spaces
        Map<String, Object> toFixNonTuned = searchSpaceToFix.getNonTunedParametersAndValues(exclusions);
        Map<String, Object> referenceNonTuned = referenceSearchSpace.getNonTunedParametersAndValues(exclusions);

        for (String param : toFixNonTuned.keySet()) {
            boolean failure = false;
            if (referenceNonTuned.containsKey(param)) {
                Object toFixVal = toFixNonTuned.get(param);
                Object refVal = referenceNonTuned.get(param);

                if (toFixVal != null && !toFixVal.equals(refVal)) {
                    System.err.printf("Warning: Non-tuned parameter '%s' has different values: ToFixSS=%s, ReferenceSS=%s%n",
                            param, toFixVal, refVal);
                    failure = true;
                }
            }
            if (failure) {
                throw new AssertionError("Incompatible Action and Value default parameters - adjust search spaces");
            }
        }
    }
}
