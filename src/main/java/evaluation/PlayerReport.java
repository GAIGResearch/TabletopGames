package evaluation;

import core.AbstractPlayer;
import core.*;
import core.interfaces.IStatisticLogger;
import games.*;
import players.PlayerFactory;
import players.simple.RandomPlayer;
import utilities.FileStatsLogger;
import utilities.SummaryLogger;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;

import static java.util.stream.Collectors.*;
import static utilities.Utils.*;

public class PlayerReport {

    /**
     * The idea here is that we get statistics from the the decisions of a particular agent in
     * a game, or set of games
     *
     * @param args
     */
    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.isEmpty() || argsList.contains("--help") || argsList.contains("-h")) System.out.println(
                "There are a number of possible arguments:\n" +
                        "\tgames=         A list of the games to be played. If there is more than one, then use a \n" +
                        "\t               pipe-delimited list, for example games=Uno|ColtExpress|Pandemic. No default.\n" +
                        "\tplayer=        The JSON file containing the details of the Player to monitor, OR\n" +
                        "\t               one of mcts|rmhc|random|osla|<className>.\n" +
                        "\tlogger=        The full class name of an IStatisticsLogger implementation.\n" +
                        "\t               Defaults to SummaryLogger. \n" +
                        "\tlogFile=       Will be used as the IStatisticsLogger log file (FileStatsLogger only)\n" +
                        "\tnPlayers=      The total number of players in each game (the default is game.Min#players) \n " +
                        "\t               Different player counts can be specified for each game in pipe-delimited format.\n" +
                        "\tnGames=        The number of games to run for each game type. Defaults to 1.\n" +
                        "\topponent=      The agent used as opponent. Default is a Random player. \n" +
                        "\t               This can either be a json-format file detailing the parameters, or\n" +
                        "\t               one of mcts|rmhc|random|osla|<className>  \n" +
                        "\t               If className is specified, this must be the full name of a class implementing AbstractPlayer\n" +
                        "\t               with a no-argument constructor\n"
        );

        // Now I get the Player and Opponent to be used
        String opponentDescriptor = getArg(args, "opponent", "");
        String playerDescriptor = getArg(args, "player", "");
        if (playerDescriptor.equals(""))
            throw new IllegalArgumentException("Must specify a player file or type");
        AbstractPlayer playerToTrack = PlayerFactory.createPlayer(playerDescriptor);

        String loggerClass = getArg(args, "logger", "utilities.SummaryLogger");

        int nGames = getArg(args, "nGames", 1);
        List<String> games = Arrays.asList(getArg(args, "games", "").split("\\|"));
        if (games.isEmpty())
            throw new IllegalArgumentException("Must specify at least one game");

        List<Integer> nPlayers = Arrays.stream(getArg(args, "nPlayers", "").split("\\|"))
                .mapToInt(Integer::valueOf).boxed().collect(toList());

        if (nPlayers.size() > 1 && nPlayers.size() != games.size())
            throw new IllegalArgumentException("If specified, then nPlayers length must be one, or match the length of the games list");

        String logFile = getArg(args, "logFile", "PlayerReport.txt");

        // Then iterate over the Game Types
        for (int gameIndex = 0; gameIndex < games.size(); gameIndex++) {
            GameType gameType = GameType.valueOf(games.get(gameIndex));
            System.out.println("Game: " + gameType.name());

            // For each type, instantiate a new IStatisticsLogger
            String fullFileName = gameType.name() + "_" + logFile;
            if (loggerClass.contains("Summary"))
                fullFileName = logFile;
            IStatisticLogger logger = IStatisticLogger.createLogger(loggerClass, fullFileName);
            if (!(logger instanceof FileStatsLogger))
                logger.record("Game", games.get(gameIndex));
            playerToTrack.setStatsLogger(logger);

            int playerCount = gameType.getMinPlayers();
            if (nPlayers.size() == 1) playerCount = nPlayers.get(0);
            if (nPlayers.size() > 1) playerCount = nPlayers.get(gameIndex);

            Game game = gameType.createGameInstance(playerCount);
            for (int i = 0; i < nGames; i++) {
                // Set up opponents and the tracked player
                // We can reduce variance here by cycling the playerIndex on each iteration
                int playerIndex = i % playerCount;
                List<AbstractPlayer> allPlayers = new ArrayList<>();
                for (int j = 0; j < playerCount; j++) {
                    if (j == playerIndex)
                        allPlayers.add(playerToTrack);
                    else
                        allPlayers.add(opponentDescriptor.isEmpty() ? new RandomPlayer() : PlayerFactory.createPlayer(opponentDescriptor));
                }
                // Run games, resetting the player each time
                game.reset(allPlayers);
                playerToTrack.initializePlayer(game.getGameState());
                game.run();
                playerToTrack.getStatsLogger().record("Win", game.getGameState().getPlayerResults()[playerIndex].value);
                playerToTrack.getStatsLogger().record("Score", game.getGameState().getScore(playerIndex));
            }
            // Once all games are complete, call processDataAndFinish()
            playerToTrack.getStatsLogger().processDataAndFinish();
        }
    }

}
