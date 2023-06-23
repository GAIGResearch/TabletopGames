package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.listeners.IGameListener;
import evaluation.loggers.SummaryLogger;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.PlayerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
                            "\tgameParams=    (Optional) A JSON file from which the game parameters will be initialised.\n" +
                            "\tnGames=        The total number of games to run in each iteration. Defaults to 100.\n" +
                            "\tstartBudget=   The budget to use for the first agent. Defaults to 8.\n" +
                            "\tmultiplier=    The factor by which to increase the budget each iteration. Defaults to 2.\n" +
                            "\titerations=    The number of iterations to run. Defaults to 5.\n" +
                            "\tdestDir=       The directory to which the results will be written. Defaults to 'metrics/out'.\n" +
                            "\t               A suffix equal to the higher budget in each iteration will be added\n" +
                            "\taddTimestamp=  If true (default), then the results will be written to a subdirectory of destDir.\n" +
                            "\tlistener=      The full class name of an IGameListener implementation. Or the location\n" +
                            "\t               of a json file from which a listener can be instantiated.\n" +
                            "\t               Defaults to evaluation.metrics.GameStatisticsListener. \n" +
                            "\t               A pipe-delimited string can be provided to gather many types of statistics \n" +
                            "\t               from the same set of games.\n"
            );
            return;
        }

        int gamesPerIteration = getArg(args, "nGames", 100);
        int startingTimeBudget = getArg(args, "startBudget", 8);
        int iterations = getArg(args, "iterations", 5);
        int timeBudgetMultiplier = getArg(args, "multiplier", 2);
        String player = getArg(args, "player", "");
        if (player.isEmpty()) {
            System.out.println("Please specify a player");
            System.exit(0);
        }
        String destDir = getArg(args, "destDir", "metrics/out");
        boolean addTimestamp = getArg(args, "addTimestamp", true);
        String gameParams = getArg(args, "gameParams", "");
        List<String> listenerClasses = new ArrayList<>(Arrays.asList(getArg(args, "listener", "evaluation.listeners.MetricsGameListener").split("\\|")));

        int nPlayers = getArg(args, "nPlayers", 2);
        String game = getArg(args, "game", "TicTacToe");
        GameType gameType = GameType.valueOf(game);
        AbstractParameters params = AbstractParameters.createFromFile(gameType, gameParams);

        int oldBudget = startingTimeBudget;
        for (int i = 0; i < iterations; i++) {
            List<AbstractPlayer> agents = new ArrayList<>(2);
            int budgetBeingTested = oldBudget * timeBudgetMultiplier;
            int finalOldBudget = oldBudget;
            AbstractPlayer baseAgent = PlayerFactory.createPlayer(player, s -> s.replaceAll("-999", Integer.toString(finalOldBudget)));
            baseAgent.setName("Budget " + finalOldBudget);
            AbstractPlayer newAgent = PlayerFactory.createPlayer(player, s -> s.replaceAll("-999", String.valueOf(budgetBeingTested)));
            newAgent.setName("Budget " + budgetBeingTested);
            agents.add(newAgent);
            agents.add(baseAgent);
            // for each iteration we run a round robin tournament

            RoundRobinTournament RRT = new RoundRobinTournament(agents, gameType, nPlayers, gamesPerIteration / nPlayers,
                    ONE_VS_ALL, params);
            RRT.verbose = false;
            StringBuilder timeDir = new StringBuilder(gameType.name() + "_" + nPlayers + "P_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
            for (String listenerClass : listenerClasses) {
                IGameListener gameTracker = IGameListener.createListener(listenerClass, null);
                RRT.getListeners().add(gameTracker);
                if (addTimestamp)
                    gameTracker.setOutputDirectory(destDir,  "Budget_" + budgetBeingTested, timeDir.toString());
                else
                    gameTracker.setOutputDirectory(destDir, "Budget_" + budgetBeingTested);
            }

            long startTime = System.currentTimeMillis();
            RRT.runTournament();
            long endTime = System.currentTimeMillis();;
            System.out.printf("%d games in %3d minutes\tBudget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f\tvs Budget %5d win rate: %.1f%% +/- %.1f%%, mean rank %.1f +/- %.1f%n",
                    (gamesPerIteration / nPlayers) * nPlayers, (endTime - startTime) / 60000,
                    budgetBeingTested,
                    RRT.getWinRate(0) * 100, RRT.getWinStdErr(0) * 100 * 2,
                    RRT.getOrdinalRank(0), RRT.getOrdinalStdErr(0) * 2,
                    budgetBeingTested / timeBudgetMultiplier,
                    RRT.getWinRate(1) * 100, RRT.getWinStdErr(1) * 100 * 2,
                    RRT.getOrdinalRank(1), RRT.getOrdinalStdErr(1) * 2
                    );
            oldBudget = budgetBeingTested;
        }
    }

}
