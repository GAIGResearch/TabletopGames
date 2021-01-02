package evaluation;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IStatisticLogger;
import games.GameType;
import players.PlayerFactory;
import utilities.*;

import java.util.*;
import java.util.stream.IntStream;

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
        if (argsList.isEmpty() || argsList.contains("--help") || argsList.contains("-h")) System.out.println(
                "There are a number of possible arguments:\n" +
                        "\tgames=         A list of the games to be played. If there is more than one, then use a \n" +
                        "\t               pipe-delimited list, for example games=Uno|ColtExpress|Pandemic.\n" +
                        "\t               'all' can be used to indicate that all games should be analysed.\n" +
                        "\tplayer=        The JSON file containing the details of the Player to monitor, OR\n" +
                        "\t               one of mcts|rmhc|random|osla|<className>. The default is 'random'.\n" +
                        "\tlogger=        The full class name of an IStatisticsLogger implementation.\n" +
                        "\t               Defaults to SummaryLogger. \n" +
                        "\tlogFile=       Will be used as the IStatisticsLogger log file (FileStatsLogger only)\n" +
                        "\tnPlayers=      The total number of players in each game (the default is game.Min#players) \n " +
                        "\t               Different player counts can be specified for each game in pipe-delimited format.\n" +
                        "\t               If 'all' is specified, then every possible playerCount for the game will be analysed.\n" +
                        "\tnGames=        The number of games to run for each game type. Defaults to 1000.\n"
        );

        // Get Player to be used
        String playerDescriptor = getArg(args, "player", "random");
        String loggerClass = getArg(args, "logger", "utilities.SummaryLogger");

        int nGames = getArg(args, "nGames", 1000);
        List<String> games = Arrays.asList(getArg(args, "games", "").split("\\|"));
        if (games.isEmpty())
            throw new IllegalArgumentException("Must specify at least one game");

        // This creates a <MinPlayer, MaxPlayer> Pair for each game#
        // TODO: Implement recognition for 'all' to apply min/max for each game
        List<Pair<Integer, Integer>> nPlayers = Arrays.stream(getArg(args, "nPlayers", "2").split("\\|"))
                .map(str -> {
                    if (str.contains("-")) {
                        int hyphenIndex = str.indexOf("-");
                        return new Pair<>(Integer.valueOf(str.substring(0, hyphenIndex)), Integer.valueOf(str.substring(hyphenIndex + 1)));
                    } else
                        return new Pair<>(Integer.valueOf(str), Integer.valueOf(str));
                }).collect(toList());

        if (nPlayers.size() > 1 && nPlayers.size() != games.size())
            throw new IllegalArgumentException("If specified, then nPlayers length must be one, or match the length of the games list");

        String logFile = getArg(args, "logFile", "GameReport.txt");

        // Then iterate over the Game Types
        for (int gameIndex = 0; gameIndex < games.size(); gameIndex++) {
            GameType gameType = GameType.valueOf(games.get(gameIndex));

            Pair<Integer, Integer> playerCounts = nPlayers.size() == 1 ? nPlayers.get(0) : nPlayers.get(gameIndex);
            int minPlayers = playerCounts.a;
            int maxPlayers = playerCounts.b;
            for (int playerCount = minPlayers; playerCount <= maxPlayers; playerCount++) {
                System.out.printf("Game: %s, Players: %d\n", gameType.name(), playerCount);
                IStatisticLogger logger = IStatisticLogger.createLogger(loggerClass, logFile);

                Map<String, Object> collectedData = new HashMap<>();
                collectedData.put("Game", games.get(gameIndex));
                collectedData.put("Players", String.valueOf(playerCount));

                Game game = gameType.createGameInstance(playerCount);
                for (int i = 0; i < nGames; i++) {
                    List<AbstractPlayer> allPlayers = new ArrayList<>();
                    for (int j = 0; j < playerCount; j++) {
                        allPlayers.add(PlayerFactory.createPlayer(playerDescriptor));
                    }
                    // Run games, resetting the player each time
                    game.reset(allPlayers);
                    preGameProcessing(game, collectedData);
                    game.run();
                    postGameProcessing(game, collectedData);
                    logger.record(collectedData);
                }
                // Once all games are complete, call processDataAndFinish()
                logger.processDataAndFinish();
            }
        }
    }

    private static void preGameProcessing(Game game, Map<String, Object> data) {
        AbstractGameState gs = game.getGameState();

        AbstractForwardModel fm = game.getForwardModel();
        long s = System.nanoTime();
        fm.setup(gs);
        data.put("SetupTime", (System.nanoTime() - s) / 10e3);

        int totalComponents = gs.getAllComponents().size();
        data.put("StateSize", totalComponents);

        IntStream.range(0, game.getPlayers().size()).forEach(p -> {
            int unseen = gs.getUnknownComponentsIds(p).size();
            data.put("Hidden_P" + p, unseen / (double) totalComponents);
        });
    }

    private static void postGameProcessing(Game game, Map<String, Object> data) {

        //    Retrieves a list with one entry per game tick, each a pair (active player ID, # actions)
        List<Pair<Integer, Integer>> actionSpaceRecord = game.getActionSpaceSize();
        IntSummaryStatistics stats = actionSpaceRecord.stream()
                .mapToInt(r -> r.b)
                .summaryStatistics();
        data.put("MeanActionSpace", stats.getAverage());
        data.put("MinActionSpace", stats.getMin());
        data.put("MaxActionSpace", stats.getMax());
        data.put("Actions", stats.getCount());
        data.put("NextTime",  game.getNextTime() / 10e3);
        data.put("CopyTime",  game.getCopyTime() / 10e3);
        data.put("ActionComputeTime", game.getActionComputeTime() / 10e3);
        data.put("AgentTime", game.getAgentTime() / 10e3);

        data.put("Decisions", game.getNDecisions());
        data.put("Ticks", game.getTick());
        data.put("Rounds", game.getGameState().getTurnOrder().getRoundCounter());
        data.put("ActionsPerTurn", game.getNActionsPerTurn());

        IntStream.range(0, game.getPlayers().size()).forEach(p -> {
            IntSummaryStatistics playerStats = actionSpaceRecord.stream()
                    .filter(r -> r.a == p)
                    .mapToInt(r -> r.b)
                    .summaryStatistics();
            data.put("MeanActionSpace_P" + p, playerStats.getAverage());
            data.put("MinActionSpace_P" + p, playerStats.getMin());
            data.put("MaxActionSpace_P" + p, playerStats.getMax());
            data.put("Decisions_P" + p, playerStats.getCount());
        });

    }
}
