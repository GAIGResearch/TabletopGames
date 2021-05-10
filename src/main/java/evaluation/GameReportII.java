package evaluation;

import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import games.GameType;
import players.PlayerFactory;
import utilities.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static utilities.Utils.getArg;

public class GameReportII {

    /**
     * The idea here is that we get statistics from the the decisions of a particular agent in
     * a game, or set of games
     *
     * @param args
     */
    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            System.out.println(
                    "There are a number of possible arguments:\n" +
                            "\tgames=         A list of the games to be played. If there is more than one, then use a \n" +
                            "\t               pipe-delimited list, for example games=Uno|ColtExpress|Pandemic.\n" +
                            "\t               The default is 'all' to indicate that all games should be analysed.\n" +
                            "\tplayer=        The JSON file containing the details of the Player to monitor, OR\n" +
                            "\t               one of mcts|rmhc|random|osla|<className>. The default is 'random'.\n" +
                            "\tlistener=      The full class name of an IGameListener implementation. \n" +
                            "\t               Defaults to utilities.GameReportListener. \n" +
                            "\t               A pipe-delimited string can be provided to gather many types of statistics \n" +
                            "\t               from the same set of games." +
                            "\tlogger=        The full class name of an IStatisticsLogger implementation.\n" +
                            "\t               Defaults to utilities.SummaryLogger. \n" +
                            "\tlogFile=       Will be used as the IStatisticsLogger log file (FileStatsLogger only)\n" +
                            "\t               A pipe-delimited list should be provided if each distinct listener should\n" +
                            "\t               use a different log file.\n" +
                            "\tnPlayers=      The total number of players in each game (the default is 'all') \n " +
                            "\t               A range can also be specified, for example 3-5. \n " +
                            "\t               Different player counts can be specified for each game in pipe-delimited format.\n" +
                            "\t               If 'all' is specified, then every possible playerCount for the game will be analysed.\n" +
                            "\tnGames=        The number of games to run for each game type. Defaults to 1000.\n"
            );
            return;
        }

        // Get Player to be used
        String playerDescriptor = getArg(args, "player", "random");
        String loggerClass = getArg(args, "logger", "utilities.SummaryLogger");
        List<String> listenerClasses = new ArrayList<>(Arrays.asList(getArg(args, "listener", "utilities.GameReportListener").split("\\|")));
        List<String> logFiles = new ArrayList<>(Arrays.asList(getArg(args, "logFile", "GameReport.txt").split("\\|")));

        if (listenerClasses.size() > 1 && logFiles.size() > 1 && listenerClasses.size() != logFiles.size())
            throw new IllegalArgumentException("Lists of log files and listeners must be the same length");

        int nGames = getArg(args, "nGames", 1000);
        List<String> games = new ArrayList<>(Arrays.asList(getArg(args, "games", "all").split("\\|")));
        if (games.get(0).equals("all"))
            games = Arrays.stream(GameType.values()).map(Enum::name).collect(toList());

        // This creates a <MinPlayer, MaxPlayer> Pair for each game#
        List<Pair<Integer, Integer>> nPlayers = Arrays.stream(getArg(args, "nPlayers", "all").split("\\|"))
                .map(str -> {
                    if (str.contains("-")) {
                        int hyphenIndex = str.indexOf("-");
                        return new Pair<>(Integer.valueOf(str.substring(0, hyphenIndex)), Integer.valueOf(str.substring(hyphenIndex + 1)));
                    } else if (str.equals("all")) {
                        return new Pair<>(-1, -1); // this is later interpreted as "All the player counts"
                    } else
                        return new Pair<>(Integer.valueOf(str), Integer.valueOf(str));
                }).collect(toList());

        // if only one game size was provided, then it applies to all games in the list
        if (games.size() == 1 && nPlayers.size() > 1) {
            for (int loop = 0; loop < nPlayers.size() - 1; loop++)
                games.add(games.get(0));
        }
        if (nPlayers.size() > 1 && nPlayers.size() != games.size())
            throw new IllegalArgumentException("If specified, then nPlayers length must be one, or match the length of the games list");

        // Then iterate over the Game Types
        for (int gameIndex = 0; gameIndex < games.size(); gameIndex++) {
            GameType gameType = GameType.valueOf(games.get(gameIndex));

            Pair<Integer, Integer> playerCounts = nPlayers.size() == 1 ? nPlayers.get(0) : nPlayers.get(gameIndex);
            int minPlayers = playerCounts.a > -1 ? playerCounts.a : gameType.getMinPlayers();
            int maxPlayers = playerCounts.b > -1 ? playerCounts.b : gameType.getMaxPlayers();
            for (int playerCount = minPlayers; playerCount <= maxPlayers; playerCount++) {
                System.out.printf("Game: %s, Players: %d\n", gameType.name(), playerCount);
                if (gameType.getMinPlayers() > playerCount) {
                    System.out.printf("Skipping game - minimum player count is %d%n", gameType.getMinPlayers());
                    continue;
                }
                if (gameType.getMaxPlayers() < playerCount) {
                    System.out.printf("Skipping game - maximum player count is %d%n", gameType.getMaxPlayers());
                    continue;
                }

                Game game = gameType.createGameInstance(playerCount);

                List<IGameListener> gameTrackers = new ArrayList<>();
                for (int i = 0; i < listenerClasses.size(); i++) {
                    String logFile = logFiles.size() == 1 ? logFiles.get(0) : logFiles.get(i);
                    String listenerClass = listenerClasses.size() == 1 ? listenerClasses.get(0) : listenerClasses.get(i);
                    IStatisticLogger logger = IStatisticLogger.createLogger(loggerClass, logFile);
                    IGameListener gameTracker = IGameListener.createListener(listenerClass, logger);
                    game.addListener(gameTracker);
                    gameTrackers.add(gameTracker);
                }

                for (int i = 0; i < nGames; i++) {
                    List<AbstractPlayer> allPlayers = new ArrayList<>();
                    for (int j = 0; j < playerCount; j++) {
                        allPlayers.add(PlayerFactory.createPlayer(playerDescriptor));
                    }
                    // Run games, resetting the player each time

                    game.reset(allPlayers);
                    game.run();
                }
                // Once all games are complete, let the gameTracker know
                for (IGameListener gameTracker : gameTrackers) {
                    gameTracker.allGamesFinished();
                }
            }
        }
    }
}


