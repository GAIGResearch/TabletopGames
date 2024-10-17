package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.RunArg.Usage;
import evaluation.listeners.IGameListener;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.IAnyTimePlayer;
import players.PlayerFactory;
import utilities.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
                config = parseConfig(json, List.of(Usage.ParameterSearch, Usage.SkillLadder));
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
        List<AbstractPlayer> allAgents = new ArrayList<>(iterations);
        AbstractPlayer firstAgent;
        if (runNTBEA) {
            NTBEAParameters ntbeaParameters = constructNTBEAParameters(config, startingTimeBudget);
            NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
            ntbeaParameters.printSearchSpaceDetails();
            // first we tune the minimum budget against the default starting agent
            Pair<Object, int[]> results = ntbea.run();
            firstAgent = (AbstractPlayer) results.a;
            currentBestSettings = results.b;
        } else {
            // We are not tuning between rungs, and just update the budget in the player definition
            firstAgent = PlayerFactory.createPlayer(player);
        }
        firstAgent.setName("Budget " + startingTimeBudget);
        allAgents.add(firstAgent);
        int matchups = (int) config.get(RunArg.matchups);

        for (int i = 0; i < iterations; i++) {
            int newBudget = (int) (Math.pow(timeBudgetMultiplier, i + 1) * startingTimeBudget);
            if (runNTBEA) {
                NTBEAParameters ntbeaParameters = constructNTBEAParameters(config, newBudget);
                // ensure we have one repeat for each player position (to make the tournament easier)
                // we will have one from the elite set, so we need nPlayers-1 more
                NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
                AbstractPlayer benchmark = allAgents.get(i).copy();
                // and set the budget of the benchmark (NTBEA will set the budgets of the other players)
                if (benchmark instanceof IAnyTimePlayer bm) {
                    bm.setBudget(newBudget);
                }
                ntbea.setOpponents(Collections.singletonList(benchmark));
                ntbea.addElite(currentBestSettings);

                Pair<Object, int[]> results = ntbea.run();
                allAgents.add((AbstractPlayer) results.a);
                if (i == 0 || !Arrays.equals(results.b, currentBestSettings)) {
                    currentBestSettings = results.b;
                    ntbea.writeAgentJSON(currentBestSettings, destDir + File.separator + "NTBEA_Budget_" + newBudget + ".json");
                }
            } else {
                allAgents.add(PlayerFactory.createPlayer(player));
            }
            allAgents.get(i + 1).setName("Budget " + newBudget);
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
                List<AbstractPlayer> agents = Arrays.asList(allAgents.get(i + 1), allAgents.get(agentIndex));
                Map<RunArg, Object> finalConfig = new HashMap<>();
                finalConfig.put(RunArg.matchups, matchups);
                finalConfig.put(RunArg.byTeam, true);
                finalConfig.put(RunArg.budget, newBudget);
                finalConfig.put(RunArg.mode, "onevsall");
                finalConfig.put(RunArg.verbose, false);
                finalConfig.put(RunArg.gameParams, params);
                RoundRobinTournament RRT = new RoundRobinTournament(agents, gameType, nPlayers, params, finalConfig);
                for (String listenerClass : listenerClasses) {
                    if (listenerClass.isEmpty()) continue;
                    IGameListener gameTracker = IGameListener.createListener(listenerClass);
                    RRT.addListener(gameTracker);
                    if (runAgainstAllAgents) {
                        String[] nestedDirectories = new String[]{destDir, "Budget_" + newBudget + " vs Budget_" + otherBudget};
                        gameTracker.setOutputDirectory(nestedDirectories);
                    } else {
                        String[] nestedDirectories = new String[]{destDir, "Budget_" + newBudget};
                        gameTracker.setOutputDirectory(nestedDirectories);
                    }
                }

                long startTime = System.currentTimeMillis();
                RRT.run();
                long endTime = System.currentTimeMillis();
                System.out.printf("%d games in %3d minutes\tBudget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f\tvs Budget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f%n",
                        (int) config.get(RunArg.matchups), (endTime - startTime) / 60000,
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

    private static NTBEAParameters constructNTBEAParameters(Map<RunArg, Object> config, int budget) {
        double NTBEABudgetOnTournament = (double) config.get(RunArg.finalPercent); // the complement will be spent on NTBEA runs

        NTBEAParameters ntbeaParameters = new NTBEAParameters(config);
        int gameBudget = (int) config.get(RunArg.tuningBudget);
        // then we override the parameters we want to change

        int nPlayers = (int) config.get(RunArg.nPlayers);

        ntbeaParameters.destDir = ntbeaParameters.destDir + File.separator + "Budget_" + budget + File.separator + "NTBEA";
        ntbeaParameters.repeats = Math.max(nPlayers, ntbeaParameters.repeats);
        ntbeaParameters.budget = budget;

        ntbeaParameters.tournamentGames = (int) (gameBudget * NTBEABudgetOnTournament);
        ntbeaParameters.iterationsPerRun = (gameBudget - ntbeaParameters.tournamentGames) / ntbeaParameters.repeats;
        if (ntbeaParameters.mode == NTBEAParameters.Mode.StableNTBEA) {
            ntbeaParameters.iterationsPerRun /= nPlayers;
        }
        ntbeaParameters.evalGames = 0;
        ntbeaParameters.logFile = "NTBEA_Runs.log";

        return ntbeaParameters;
    }

}
