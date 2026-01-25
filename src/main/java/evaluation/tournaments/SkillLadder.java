package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.ITunableParameters;
import evaluation.RunArg;
import evaluation.RunArg.Usage;
import evaluation.listeners.IGameListener;
import evaluation.optimisation.ITPSearchSpace;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.IAnyTimePlayer;
import players.PlayerFactory;
import utilities.JSONUtils;
import utilities.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static evaluation.RunArg.evalGames;
import static evaluation.RunArg.parseConfig;

/**
 * This runs experiments to plot the skills ladder of a game.
 * Given a total budget of games, a starting budget, and an incremental factor, it will run a tournament
 * using one agent with a higher budget, and the rest with the lower budget. Equal numbers of games will be run with the
 * higher budget agent in different player positions.
 */
public class SkillLadder {

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            RunArg.printHelp(Usage.SkillLadder);
            RunArg.printHelp(Usage.ParameterSearch);
            return;
        }

        // Config
        Map<RunArg, Object> config = parseConfig(args, List.of(Usage.ParameterSearch, Usage.SkillLadder));

        String setupFile = config.getOrDefault(RunArg.config, "").toString();
        if (!setupFile.isEmpty()) {
            // Read from file instead
            try {
                FileReader reader = new FileReader(setupFile);
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                config = parseConfig(json, List.of(Usage.ParameterSearch, Usage.SkillLadder), true);
            } catch (FileNotFoundException ignored) {
                throw new AssertionError("Config file not found : " + setupFile);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        List<String> listenerClasses = (List<String>) config.get(RunArg.listener);

        GameType gameType = GameType.valueOf(config.get(RunArg.game).toString());
        int nPlayers = (int) config.get(RunArg.nPlayers);
        AbstractParameters params = AbstractParameters.createFromFile(gameType, config.get(RunArg.gameParams).toString());

        int[] currentBestSettings = new int[0];

        int iterations = (int) config.get(RunArg.iterations);
        int startingTimeBudget = (int) config.get(RunArg.startBudget);
        int timeBudgetMultiplier = (int) config.get(RunArg.multiplier);
        int startGridBudget = (int) config.get(RunArg.gridStart);
        int startMinorGridBudget = (int) config.get(RunArg.gridMinorStart);
        boolean runNTBEA = (int) config.get(RunArg.tuningBudget) > 0;
        String destDir = (String) config.get(RunArg.destDir);

        String player = (String) config.get(RunArg.opponent);
        List<Pair<AbstractPlayer, int[]>> allAgents = new ArrayList<>(iterations);
        if (runNTBEA) {
            NTBEAParameters ntbeaParameters = constructNTBEAParameters(config, startingTimeBudget);
            NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
            // If NTBEA has already run we skip this step (after reading in the agent
            // and adding it to allAgents).
            if (ntbea.hasAlreadyRun() && ntbeaParameters.searchSpace instanceof ITPSearchSpace<?> itp) {
                currentBestSettings = itp.settingsFromJSON(ntbea.finalFilename());
                AbstractPlayer agent = (AbstractPlayer) itp.instantiate(currentBestSettings);
                allAgents.add(Pair.of(agent, currentBestSettings));
                System.out.println("NTBEA for budget of " + startingTimeBudget + " has already completed - skipping");
            } else { // RunNTBEA
                ntbeaParameters.printSearchSpaceDetails();
                // first we tune the minimum budget against the default starting agent
                Pair<Object, int[]> results = ntbea.run();
                allAgents.add(new Pair<>((AbstractPlayer) results.a, results.b));
                currentBestSettings = results.b;
            }
        } else {
            // We are not tuning between rungs, and just update the budget in the player definition
            allAgents.add(new Pair<>(PlayerFactory.createPlayer(player), new int[0]));
        }
        allAgents.get(0).a.setName("Budget " + startingTimeBudget);
        int matchups = (int) config.get(RunArg.matchups);

        for (int i = 0; i < iterations; i++) {
            int newBudget = (int) (Math.pow(timeBudgetMultiplier, i + 1) * startingTimeBudget);
            if (runNTBEA) {
                NTBEAParameters ntbeaParameters = constructNTBEAParameters(config, newBudget);
                // ensure we have one repeat for each player position (to make the tournament easier)
                // we will have one from the elite set, so we need nPlayers-1 more
                NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
                // If NTBEA has already run for this rung, then read in the final result and skip it
                if (ntbea.hasAlreadyRun() && ntbeaParameters.searchSpace instanceof ITPSearchSpace<?> itp) {
                    currentBestSettings = itp.settingsFromJSON(ntbea.finalFilename());
                    AbstractPlayer agent = (AbstractPlayer) itp.instantiate(currentBestSettings);
                    allAgents.add(Pair.of(agent, currentBestSettings));
                    System.out.println("NTBEA for budget of " + newBudget + " has already completed - skipping");
                } else {
                    AbstractPlayer benchmark = allAgents.get(i).a.copy();
                    // and set the budget of the benchmark (NTBEA will set the budgets of the other players)
                    if (benchmark instanceof IAnyTimePlayer bm) {
                        bm.setBudget(newBudget);
                    }
                    ntbea.setOpponents(Collections.singletonList(benchmark));
                    ntbea.addElite(currentBestSettings);

                    Pair<Object, int[]> results = ntbea.run();
                    allAgents.add(Pair.of((AbstractPlayer) results.a, results.b));
                    if (i == 0 || !Arrays.equals(results.b, currentBestSettings)) {
                        currentBestSettings = results.b;
                        ntbea.writeAgentJSON(currentBestSettings, destDir + File.separator + "NTBEA_Budget_" + newBudget + ".json");
                    }
                }
            } else {
                allAgents.add(new Pair<>(PlayerFactory.createPlayer(player), new int[0]));
            }
            allAgents.get(i + 1).a.setName("Budget " + newBudget);
            if (newBudget < startGridBudget) // we fast forward to where we want to start the grid
                continue;
            if (matchups == 0)
                continue; // we are just using this for progressive NTBEA tuning

            // for each iteration we run a round robin tournament; either against just the previous agent (with the previous budget), or
            // if we have grid set to true, then against all previous agents, one after the other
            boolean runAgainstAllAgents = (boolean) config.get(RunArg.grid);
            int startAgent = runAgainstAllAgents ? 0 : i;
            for (int agentIndex = startAgent; agentIndex <= i; agentIndex++) {
                int otherBudget = (int) (Math.pow(timeBudgetMultiplier, agentIndex) * startingTimeBudget);
                if (newBudget == startGridBudget && otherBudget < startMinorGridBudget) // we fast forward to where we want to start the minor grid
                    continue;
                List<AbstractPlayer> agents = Arrays.asList(allAgents.get(i + 1).a.copy(), allAgents.get(agentIndex).a.copy());
                ((IAnyTimePlayer) agents.get(0)).setBudget(newBudget);
                ((IAnyTimePlayer) agents.get(1)).setBudget(otherBudget);

                // If the output file from the Tournament already exists, then
                // we skip this, and log that the tournament has already been run.
                String outputDirectory = destDir + File.separator +
                        (runAgainstAllAgents ? "Budget_" + newBudget + "_vs_" + otherBudget : "Budget_" + newBudget);
                if ((new File(outputDirectory + File.separator + "TournamentResults.txt")).exists()) {
                    System.out.println("Skipping Tournament as already run : " + outputDirectory);
                } else {
                    long startTime = System.currentTimeMillis();
                    RoundRobinTournament RRT = runRoundRobinTournament(agents, 0, matchups, listenerClasses,
                            gameType, nPlayers, params, "onevsall", outputDirectory);
                    long endTime = System.currentTimeMillis();

                    System.out.printf("%d games in %3d minutes\tBudget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f\tvs Budget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f%n",
                            matchups, (endTime - startTime) / 60000,
                            newBudget,
                            RRT.getWinRate(0) * 100, RRT.getWinStdErr(0) * 100 * 2,
                            RRT.getOrdinalRank(0), RRT.getOrdinalStdErr(0) * 2,
                            otherBudget,
                            RRT.getWinRate(1) * 100, RRT.getWinStdErr(1) * 100 * 2,
                            RRT.getOrdinalRank(1), RRT.getOrdinalStdErr(1) * 2
                    );
                }
            }
        }
        // As a final step we run a tournament for each budget with each (unique) agent from any of the budgets
        // We use matchups x nPlayers for this (somewhat arbitrarily...but we should use more than matchups as we are trying to resolve across more than just 2 agents)
        // If there is only one, then we can skip this
        // If the number is less than the number of players, then we additionally set exhaustiveSP to true on the tournament

        // As a first step we work out the unique agents and write these to a specific directory

        List<Pair<AbstractPlayer, int[]>> uniqueAgents = getAgentsWithUniqueSettings(allAgents);
        String uniqueAgentsDir = destDir + File.separator + "UniqueAgents";
        new File(uniqueAgentsDir).mkdirs();
        for (int i = 0; i < iterations; i++) {
            int newBudget = (int) (Math.pow(timeBudgetMultiplier, i + 1) * startingTimeBudget);
            NTBEA ntbea = new NTBEA(constructNTBEAParameters(config, newBudget), gameType, nPlayers);
            if (i == 0) {
                // add unique agent files just once for information
                for (int n = 0; n < uniqueAgents.size(); n++) {
                    uniqueAgents.get(n).a.setName(gameType.name() + "_" + nPlayers + "P_" + n);
                    ntbea.writeAgentJSON(uniqueAgents.get(n).b, uniqueAgentsDir + File.separator + uniqueAgents.get(n).a.toString() + ".json");
                }
            }
            // now run the tournament
            List<AbstractPlayer> agents = uniqueAgents.stream().map(p -> p.a.copy()).toList();
            // set the budget of the agents
            for (int n = 0; n < uniqueAgents.size(); n++) {
                if (agents.get(n) instanceof IAnyTimePlayer bm) {
                    bm.setBudget(newBudget);
                }
            }

            // number of games in the final tournament for each budget is the largest of:
            // - matchups * nPlayers, or
            // - the number of games run to judge relative performance at each budget between the repeat winners
            int gamesToRun = Math.max(Math.max(matchups, (int) config.get(RunArg.evalGames)) * nPlayers,
                    (int) ((int) config.get(RunArg.tuningBudget) * (double) config.get(RunArg.finalPercent)));
            RoundRobinTournament RRT = runRoundRobinTournament(agents, newBudget, gamesToRun,
                    listenerClasses, gameType, nPlayers, params,
                    agents.size() >= nPlayers ? "exhaustive" : "exhaustiveSP",
                    destDir + File.separator + "Final_Budget_" + newBudget);

            // then write JSON of winner
            int winnerIndex = RRT.getWinnerIndex();
            ntbea.writeAgentJSON(uniqueAgents.get(winnerIndex).b, destDir + File.separator +
                    gameType.name() + "_" + nPlayers + "P_" + String.format("%04d", newBudget) + "ms.json");
        }

    }

    private static List<Pair<AbstractPlayer, int[]>> getAgentsWithUniqueSettings(List<Pair<AbstractPlayer, int[]>> allAgents) {
        List<Pair<AbstractPlayer, int[]>> uniqueAgentsAndSettings = new ArrayList<>();
        for (Pair<AbstractPlayer, int[]> agent : allAgents) {
            boolean found = false;
            for (Pair<AbstractPlayer, int[]> uniqueAgent : uniqueAgentsAndSettings) {
                if (Arrays.equals(agent.b, uniqueAgent.b)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                uniqueAgentsAndSettings.add(agent);
            }
        }
        return uniqueAgentsAndSettings;
    }

    private static NTBEAParameters constructNTBEAParameters(Map<RunArg, Object> config, int budget) {
        double NTBEABudgetOnTournament = (double) config.get(RunArg.finalPercent); // the complement will be spent on NTBEA runs

        NTBEAParameters ntbeaParameters = new NTBEAParameters(config);
        int gameBudget = (int) config.get(RunArg.tuningBudget);
        // then we override the parameters we want to change

        int nPlayers = (int) config.get(RunArg.nPlayers);

        ntbeaParameters.destDir = ntbeaParameters.destDir + File.separator + "Budget_" + budget + File.separator + "NTBEA";
        ntbeaParameters.setParameterValue("repeats", Math.max(nPlayers, ntbeaParameters.repeats));
        ntbeaParameters.setParameterValue("budget", budget);

        ntbeaParameters.setParameterValue("matchups", (int) (gameBudget * NTBEABudgetOnTournament));
        ntbeaParameters.setParameterValue("iterations", (gameBudget - ntbeaParameters.tournamentGames) / ntbeaParameters.repeats);
        if (ntbeaParameters.mode == NTBEAParameters.Mode.StableNTBEA) {
            ntbeaParameters.setParameterValue("iterations", ntbeaParameters.iterationsPerRun /= nPlayers);
        }
        ntbeaParameters.setParameterValue("evalGames", 0);
        ntbeaParameters.logFile = "NTBEA_Runs.log";

        return ntbeaParameters;
    }

    private static RoundRobinTournament runRoundRobinTournament(List<AbstractPlayer> agents, int budget,
                                                                int matchups, List<String> listenerClasses, GameType gameType,
                                                                int nPlayers, AbstractParameters params, String mode,
                                                                String destDir) {
        Map<RunArg, Object> finalConfig = new HashMap<>();
        finalConfig.put(RunArg.matchups, matchups);
        finalConfig.put(RunArg.destDir, destDir);
        finalConfig.put(RunArg.byTeam, true);
        finalConfig.put(RunArg.budget, budget);
        finalConfig.put(RunArg.mode, mode);
        finalConfig.put(RunArg.verbose, false);
        finalConfig.put(RunArg.gameParams, params);
        RoundRobinTournament RRT = new RoundRobinTournament(agents, gameType, nPlayers, params, finalConfig);
        for (String listenerClass : listenerClasses) {
            if (listenerClass.isEmpty()) continue;
            IGameListener gameTracker = IGameListener.createListener(listenerClass);
            RRT.addListener(gameTracker);
            gameTracker.setOutputDirectory(destDir);
        }

        RRT.run();
        return RRT;
    }

}
