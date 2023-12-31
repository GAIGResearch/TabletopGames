package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.RunGames;
import evaluation.listeners.IGameListener;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.IAnyTimePlayer;
import players.PlayerFactory;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static evaluation.RunArg.*;
import static evaluation.tournaments.AbstractTournament.TournamentMode.ONE_VS_ALL;
import static utilities.Utils.getArg;

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
            System.out.println(
                    "There are a number of possible arguments:\n" +
                            "\tgame=          The name of the game to play. Defaults to TicTacToe.\n" +
                            "\tnPlayers=      The number of players in each game. Defaults to 2.\n" +
                            "\tplayer=        The JSON file that contains the base configuration to use.\n" +
                            "\t               If searchSpace is not specified, then also use -999 \n" +
                            "\t               for any budget parameter - this will be replaced with the budget to use.\n" +
                            "\tsearchSpace=   The JSON file that contains the search space to use for NTBEA tuning.\n" +
                            "\t               Use -999 for any budget parameter - this will be replaced with the budget to use.\n" +
                            "\t               If searchSpace is specified, then player will be used only to tune the lowest budget agent\n" +
                            "\t               and will not be used otherwise - all tournaments in the ladder will use tuned agents using the searchspace.\n" +
                            "\tgameParams=    (Optional) A JSON file from which the game parameters will be initialised.\n" +
                            "\tnGames=        The total number of games to run for each rung of the ladder. Defaults to 1000.\n" +
                            "\tstartBudget=   The budget to use for the first agent. Defaults to 8.\n" +
                            "\tmultiplier=    The factor by which to increase the budget each iteration. Defaults to 2.\n" +
                            "\titerations=    The number of iterations to run. Defaults to 5.\n" +
                            "\tdestDir=       The directory to which the results will be written. Defaults to 'metrics/out'.\n" +
                            "\t               A suffix equal to the higher budget in each iteration will be added\n" +
                            "\tlistener=      The full class name of an IGameListener implementation. Or the location\n" +
                            "\t               of a json file from which a listener can be instantiated.\n" +
                            "\t               A pipe-delimited string can be provided to gather many types of statistics \n" +
                            "\t               from the same set of games.\n" +
                            "\tNTBEABudget=   The budget of games to use for NTBEA tuning at each budget count. Defaults to 0.\n" +
                            "\t               If specified, then player is a searchSpace definition, and we use random as the lowest budget.\n" +
                            "\t               The default is to spend 50% on tuning, and 50% on the final tournament to pick the best.\n" +
                            "\tgrid=          Default false. If true, then we run against all previous agents too.\n" +
                            "\tgridStart=     (Optional). The budget at which to start...the startBudget is still relevant\n" +
                            "\t               for the opponents against which we test. Do not use with NTBEABudget.\n" +
                            "\tgridMinorStart=(Optional). The budget at which to start the lower grid budget. Do not use with NTBEABudget.\n" +
                            "\tstartSettings= (Optional). A sequence of numbers that defines the starting agent. This is \n" +
                            "\t               primarily useful if you need to re-start the ladder from a pre-calculated rung.\n"

            );
            return;
        }

        int gamesPerIteration = getArg(args, "nGames", 100);
        int startingTimeBudget = getArg(args, "startBudget", 8);
        int iterations = getArg(args, "iterations", 5);
        int timeBudgetMultiplier = getArg(args, "multiplier", 2);
        int NTBEABudget = getArg(args, "NTBEABudget", 0);
        String player = getArg(args, "player", "");
        if (player.isEmpty()) {
            System.out.println("Please specify a player");
            System.exit(0);
        }
        String searchSpace = getArg(args, "searchSpace", "");
        if (NTBEABudget > 0 && searchSpace.isEmpty()) {
            System.out.println("Please specify a search space");
            System.exit(0);
        }
        String destDir = getArg(args, "destDir", "metrics/out");
        String gameParams = getArg(args, "gameParams", "");
        List<String> listenerClasses = new ArrayList<>(Arrays.asList(getArg(args, "listener", "").split("\\|")));

        String game = getArg(args, "game", "TicTacToe");
        GameType gameType = GameType.valueOf(game);
        int nPlayers = getArg(args, "nPlayers", gameType.getMinPlayers());
        AbstractParameters params = AbstractParameters.createFromFile(gameType, gameParams);

        String startSettings = getArg(args, "startSettings", "");
        int[] currentBestSettings = new int[0];

        boolean runAgainstAllAgents = getArg(args, "grid", false);
        int startGridBudget = getArg(args, "gridStart", startingTimeBudget);
        int startMinorGridBudget = getArg(args, "gridMinorStart", startingTimeBudget);

        List<AbstractPlayer> allAgents = new ArrayList<>(iterations);
        AbstractPlayer firstAgent;
        if (NTBEABudget > 0) {
            NTBEAParameters ntbeaParameters = constructNTBEAParameters(args, startingTimeBudget, NTBEABudget);
            ntbeaParameters.repeats = Math.max(nPlayers, ntbeaParameters.repeats);
            NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
            ntbeaParameters.printSearchSpaceDetails();
            if (startSettings.isEmpty()) {
                // first we tune the minimum budget against the default starting agent
                Pair<Object, int[]> results = ntbea.run();
                firstAgent = (AbstractPlayer) results.a;
                currentBestSettings = results.b;
            } else {
                // or we use the specified starting settings
                currentBestSettings = Arrays.stream(startSettings.split("")).mapToInt(Integer::parseInt).toArray();
                firstAgent = (AbstractPlayer) ntbeaParameters.searchSpace.getAgent(currentBestSettings);
            }
        } else {
            // We are not tuning between rungs, and just update the budget in the player definition
            firstAgent = PlayerFactory.createPlayer(player, s -> s.replaceAll("-999", Integer.toString(startingTimeBudget)));
        }
        firstAgent.setName("Budget " + startingTimeBudget);
        allAgents.add(firstAgent);

        for (int i = 0; i < iterations; i++) {
            int newBudget = (int) (Math.pow(timeBudgetMultiplier, i + 1) * startingTimeBudget);
            if (NTBEABudget > 0) {
                NTBEAParameters ntbeaParameters = constructNTBEAParameters(args, newBudget, NTBEABudget);
                // ensure we have one repeat for each player position (to make the tournament easier)
                // we will have one from the elite set, so we need nPlayers-1 more
                ntbeaParameters.repeats = Math.max(nPlayers - 1, ntbeaParameters.repeats);
                NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
                AbstractPlayer benchmark = allAgents.get(i).copy();
                if (benchmark instanceof IAnyTimePlayer) {
                    ((IAnyTimePlayer) benchmark).setBudget(newBudget);
                }
                ntbea.setOpponents(Collections.singletonList(benchmark));
                ntbea.addElite(currentBestSettings);

                Pair<Object, int[]> results = ntbea.run();
                allAgents.add((AbstractPlayer) results.a);
                currentBestSettings = results.b;
            } else {
                allAgents.add(PlayerFactory.createPlayer(player, s -> s.replaceAll("-999", String.valueOf(newBudget))));
            }
            allAgents.get(i + 1).setName("Budget " + newBudget);
            if (newBudget < startGridBudget) // we fast forward to where we want to start the grid
                continue;
            // for each iteration we run a round robin tournament; either against just the previous agent (with the previous budget), or
            // if we have grid set to true, then against all previous agents, one after the other
            int startAgent = runAgainstAllAgents ? 0 : i;
            for (int agentIndex = startAgent; agentIndex <= i; agentIndex++) {
                int otherBudget = (int) (Math.pow(timeBudgetMultiplier, agentIndex) * startingTimeBudget);
                if (newBudget == startGridBudget && otherBudget < startMinorGridBudget) // we fast forward to where we want to start the minor grid
                    continue;
                List<AbstractPlayer> agents = Arrays.asList(allAgents.get(i + 1), allAgents.get(agentIndex));
                Map<RunArg, Object> config = new HashMap<>();
                config.put(matchups, gamesPerIteration);
                config.put(byTeam, false);
                RoundRobinTournament RRT = new RoundRobinTournament(agents, gameType, nPlayers, params, ONE_VS_ALL, config);
                RRT.verbose = false;
                for (String listenerClass : listenerClasses) {
                    if (listenerClass.isEmpty()) continue;
                    IGameListener gameTracker = IGameListener.createListener(listenerClass, null);
                    RRT.getListeners().add(gameTracker);
                    if (runAgainstAllAgents) {
                        String[] nestedDirectories = new String[]{destDir, "Budget_" + newBudget + " vs Budget_" + otherBudget};
                        gameTracker.setOutputDirectory(nestedDirectories);
                    } else {
                        String[] nestedDirectories = new String[]{destDir, "Budget_" + newBudget};
                        gameTracker.setOutputDirectory(nestedDirectories);
                    }
                }

                long startTime = System.currentTimeMillis();
                RRT.setResultsFile((destDir.isEmpty() ? "" : destDir + File.separator) + "TournamentResults.txt");
                RRT.run();
                long endTime = System.currentTimeMillis();
                System.out.printf("%d games in %3d minutes\tBudget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f\tvs Budget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f%n",
                        gamesPerIteration, (endTime - startTime) / 60000,
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

    private static NTBEAParameters constructNTBEAParameters(String[] args, int agentBudget, int gameBudget) {
        int NTBEARunsBetweenRungs = 4;
        double NTBEABudgetOnTournament = 0.50; // the complement will be spent on NTBEA runs

        NTBEAParameters ntbeaParameters = new NTBEAParameters(parseConfig(args, RunArg.Usage.ParameterSearch, false), s -> s.replaceAll("-999", Integer.toString(agentBudget)));

        ntbeaParameters.destDir = ntbeaParameters.destDir + File.separator + "Budget_" + agentBudget + File.separator + "NTBEA";
        ntbeaParameters.repeats = NTBEARunsBetweenRungs;

        ntbeaParameters.tournamentGames = (int) (gameBudget * NTBEABudgetOnTournament);
        ntbeaParameters.iterationsPerRun = (gameBudget - ntbeaParameters.tournamentGames) / NTBEARunsBetweenRungs;
        ntbeaParameters.evalGames = 0;
        ntbeaParameters.opponentDescriptor = getArg(args, "player", "random");
        ntbeaParameters.logFile = "NTBEA_Runs.log";
        return ntbeaParameters;
    }

}
