package evaluation;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import games.GameType;
import players.PlayerFactory;
import utilities.Pair;
import utilities.TAGStatSummary;
import utilities.TAGSummariser;

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
        List<String> games = new ArrayList<>(Arrays.asList(getArg(args, "games", "").split("\\|")));
        if (games.isEmpty())
            throw new IllegalArgumentException("Must specify at least one game, or 'all'");
        if (games.get(0).equals("all"))
            games = Arrays.stream(GameType.values()).map(String::valueOf).collect(toList());

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

        if (games.size() == 1 && nPlayers.size() > 1) {
            for (int loop = 0; loop < nPlayers.size() - 1; loop++)
                games.add(games.get(0));
        }
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
                collectedData.put("PlayerType", playerDescriptor);

                Game game = gameType.createGameInstance(playerCount);

                for (int i = 0; i < nGames; i++) {
                    List<AbstractPlayer> allPlayers = new ArrayList<>();
                    for (int j = 0; j < playerCount; j++) {
                        allPlayers.add(PlayerFactory.createPlayer(playerDescriptor));
                    }
                    // Run games, resetting the player each time
                    game.reset(allPlayers);
                    GameReportListener gameTracker = new GameReportListener(game.getForwardModel());
                    game.addListener(gameTracker);
                    preGameProcessing(game, collectedData);
                    game.run();
                    postGameProcessing(game, collectedData);
                    game.clearListeners();
                    collectedData.putAll(gameTracker.extractData());
                    logger.record(collectedData);
                    collectedData.clear();
                }
                // Once all games are complete, call processDataAndFinish()
                logger.processDataAndFinish();
            }
        }
    }

    private static class GameReportListener implements IGameListener {

        List<Double> scores = new ArrayList<>();
        //        List<Integer> branchingByStates = new ArrayList<>();
        List<Double> visibilityOnTurn = new ArrayList<>();
        List<Integer> components = new ArrayList<>();
        AbstractForwardModel fm;

        public GameReportListener(AbstractForwardModel forwardModel) {
            fm = forwardModel;
        }

        @Override
        public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction actionChosen) {
            if (type == CoreConstants.GameEvents.ACTION_CHOSEN) {
                // each action taken, we record branching factor and states (this is triggered when the decision is made,
                // so before it is executed
                int player = state.getCurrentPlayer();
                List<AbstractAction> allActions = fm.computeAvailableActions(state);
                if (allActions.size() < 2) return;
//                HashSet<Integer> forwardStates = new HashSet<>();
//                for (AbstractAction action : allActions) {
//                    AbstractGameState gsCopy = state.copy();
//                    fm.next(gsCopy, action);
//                    forwardStates.add(gsCopy.hashCode());
//                }
//                branchingByStates.add(forwardStates.size());
                scores.add(state.getGameScore(player));
                Pair<Integer, int[]> allComp = countComponents(state);
                components.add(allComp.a);
                visibilityOnTurn.add(allComp.b[player] / (double) allComp.a);
                //          System.out.printf("Turn: %d, Player: %d, Action: %s%n", state.getTurnOrder().getTurnCounter(), player, actionChosen);
            }
        }

        public Map<String, Object> extractData() {
            Map<String, Object> data = new HashMap<>();
//            IntSummaryStatistics bf = branchingByStates.stream().mapToInt(i -> i).summaryStatistics();
//            data.put("BranchingFactor", bf.getAverage());
//            data.put("MaxBranchingFactor", bf.getMax());
            TAGStatSummary sc = scores.stream().collect(new TAGSummariser());
            data.put("ScoreMedian", sc.median());
            data.put("ScoreMean", sc.mean());
            data.put("ScoreMax", sc.max());
            data.put("ScoreMin", sc.min());
            data.put("ScoreVarCoeff", Math.abs(sc.sd() / sc.mean()));
            TAGStatSummary stateSize = components.stream().collect(new TAGSummariser());
            data.put("StateSizeMedian", stateSize.median());
            data.put("StateSizeMean", stateSize.mean());
            data.put("StateSizeMax", stateSize.max());
            data.put("StateSizeMin", stateSize.min());
            data.put("StateSizeVarCoeff", Math.abs(stateSize.sd() / stateSize.mean()));
            TAGStatSummary visibility = visibilityOnTurn.stream().collect(new TAGSummariser());
            data.put("HiddenInfoMedian", visibility.median());
            data.put("HiddenInfoMean", visibility.mean());
            data.put("HiddenInfoMax", visibility.max());
            data.put("HiddenInfoMin", visibility.min());
            data.put("HiddenInfoVarCoeff", Math.abs(visibility.sd() / visibility.mean()));
            return data;
        }

    }

    private static void preGameProcessing(Game game, Map<String, Object> data) {
        AbstractGameState gs = game.getGameState();

        AbstractForwardModel fm = game.getForwardModel();
        long s = System.nanoTime();
        fm.setup(gs);
        data.put("TimeSetup", (System.nanoTime() - s) / 10e3);

        Pair<Integer, int[]> components = countComponents(gs);
        data.put("StateSizeStart", components.a);

        IntStream.range(0, game.getPlayers().size()).forEach(p -> {
            int unseen = components.b[p];
            data.put("HiddenInfoStart", unseen / (double) components.a);
        });
    }

    /**
     * Returns the total number of components in the state as the first element of the returned value
     * and an array of the counts that are hidden to each player
     * <p>
     *
     * @param state
     * @return The total number of components
     */
    private static Pair<Integer, int[]> countComponents(AbstractGameState state) {
        int[] hiddenByPlayer = new int[state.getNPlayers()];
        int total = state.getAllComponents().size();
            for (int p = 0; p < hiddenByPlayer.length; p++)
                hiddenByPlayer[p] = state.getUnknownComponentsIds(p).size();
        return new Pair<>(total, hiddenByPlayer);
    }

    private static void postGameProcessing(Game game, Map<String, Object> data) {

        //    Retrieves a list with one entry per game tick, each a pair (active player ID, # actions)
        List<Pair<Integer, Integer>> actionSpaceRecord = game.getActionSpaceSize();
        TAGStatSummary stats = actionSpaceRecord.stream()
                .map(r -> r.b)
                .filter(size -> size > 1)
                .collect(new TAGSummariser());
        data.put("ActionSpaceMean", stats.mean());
        data.put("ActionSpaceMin", stats.min());
        data.put("ActionSpaceMedian", stats.median());
        data.put("ActionSpaceMax", stats.max());
        data.put("ActionSpaceSkew", stats.skew());
        data.put("ActionSpaceKurtosis", stats.kurtosis());
        data.put("ActionSpaceVarCoeff", Math.abs(stats.sd() / stats.mean()));
        data.put("Decisions", stats.n());
        data.put("TimeNext", game.getNextTime() / 10e3);
        data.put("TimeCopy", game.getCopyTime() / 10e3);
        data.put("TimeActionCompute", game.getActionComputeTime() / 10e3);
        data.put("TimeAgent", game.getAgentTime() / 10e3);

        data.put("Ticks", game.getTick());
        data.put("Rounds", game.getGameState().getTurnOrder().getRoundCounter());
        data.put("ActionsPerTurn", game.getNActionsPerTurn());

//        IntStream.range(0, game.getPlayers().size()).forEach(p -> {
//            StatSummary playerStats = actionSpaceRecord.stream()
//                    .filter(r -> r.a == p)
//                    .map(r -> r.b)
//                    .filter(size -> size > 1)
//                    .collect(new TAGSummariser());
//            data.put("ActionSpaceMean_P" + p, playerStats.mean());
//            data.put("ActionSpaceMin_P" + p, playerStats.min());
//            data.put("ActionSpaceMax_P" + p, playerStats.max());
//            data.put("Decisions_P" + p, playerStats.n());
//        });

    }
}


