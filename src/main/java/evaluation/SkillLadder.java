package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.listeners.IGameListener;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.IAnyTimePlayer;
import players.PlayerFactory;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.io.File;
import java.util.*;

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
                            "\t               Use -999 for any budget parameter - this will be replaced with the budget to use.\n" +
                            "\tsearchSpace=   The JSON file that contains the search space to use for NTBEA tuning.\n" +
                            "\t               Use -999 for any budget parameter - this will be replaced with the budget to use.\n" +
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
                            "\t               The default is to spend 50% on tuning, and 50% on the final tournament to pick the best.\n"

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

        AbstractPlayer baseAgent, newAgent;
        int[] currentBestSettings = new int[0];
        if (NTBEABudget > 0) {
            // first we tune the minimum budget against a random player
            NTBEAParameters ntbeaParameters = constructNTBEAParameters(args, startingTimeBudget, NTBEABudget);
            NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
            ntbeaParameters.printSearchSpaceDetails();
            Pair<Object, int[]> results = ntbea.run();
            baseAgent = (AbstractPlayer) results.a;
            currentBestSettings = results.b;
        } else {
            baseAgent = PlayerFactory.createPlayer(player, s -> s.replaceAll("-999", Integer.toString(startingTimeBudget)));
        }
        baseAgent.setName("Budget " + startingTimeBudget);

        int oldBudget = startingTimeBudget;
        for (int i = 0; i < iterations; i++) {
            List<AbstractPlayer> agents = new ArrayList<>(2);
            int newBudget = oldBudget * timeBudgetMultiplier;
            if (NTBEABudget > 0) {
                NTBEAParameters ntbeaParameters = constructNTBEAParameters(args, newBudget, NTBEABudget);
                NTBEA ntbea = new NTBEA(ntbeaParameters, gameType, nPlayers);
                AbstractPlayer benchmark = baseAgent;
                if (benchmark instanceof IAnyTimePlayer) {
                    benchmark = baseAgent.copy();
                    ((IAnyTimePlayer) benchmark).setBudget(newBudget);
                }
                ntbea.setOpponents(Collections.singletonList(benchmark));
                ntbea.addElite(currentBestSettings);

                Pair<Object, int[]> results = ntbea.run();
                newAgent = (AbstractPlayer) results.a;
                currentBestSettings = results.b;
            } else {
                newAgent = PlayerFactory.createPlayer(player, s -> s.replaceAll("-999", String.valueOf(newBudget)));
            }
            newAgent.setName("Budget " + newBudget);
            agents.add(newAgent);
            agents.add(baseAgent);
            // for each iteration we run a round robin tournament

            RoundRobinTournament RRT = new RoundRobinTournament(agents, gameType, nPlayers, gamesPerIteration / nPlayers,
                    ONE_VS_ALL, params);
            RRT.verbose = false;
            for (String listenerClass : listenerClasses) {
                if (listenerClass.isEmpty()) continue;
                IGameListener gameTracker = IGameListener.createListener(listenerClass, null);
                RRT.getListeners().add(gameTracker);
                String[] nestedDirectories = new String[]{destDir, "Budget_" + newBudget};
                gameTracker.setOutputDirectory(nestedDirectories);
            }

            long startTime = System.currentTimeMillis();
            RRT.setResultsFile((destDir.isEmpty() ? "" : destDir + File.separator) + "TournamentResults.txt");
            RRT.runTournament();
            long endTime = System.currentTimeMillis();
            System.out.printf("%d games in %3d minutes\tBudget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f\tvs Budget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f%n",
                    (gamesPerIteration / nPlayers) * nPlayers, (endTime - startTime) / 60000,
                    newBudget,
                    RRT.getWinRate(0) * 100, RRT.getWinStdErr(0) * 100 * 2,
                    RRT.getOrdinalRank(0), RRT.getOrdinalStdErr(0) * 2,
                    newBudget / timeBudgetMultiplier,
                    RRT.getWinRate(1) * 100, RRT.getWinStdErr(1) * 100 * 2,
                    RRT.getOrdinalRank(1), RRT.getOrdinalStdErr(1) * 2
            );
            oldBudget = newBudget;
            baseAgent = newAgent;
        }
    }

    private static NTBEAParameters constructNTBEAParameters(String[] args, int agentBudget, int gameBudget) {
        int NTBEARunsBetweenRungs = 4;
        double NTBEABudgetOnTournament = 0.50; // the complement will be spent on NTBEA runs

        NTBEAParameters ntbeaParameters = new NTBEAParameters(args, s -> s.replaceAll("-999", Integer.toString(agentBudget)));

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
