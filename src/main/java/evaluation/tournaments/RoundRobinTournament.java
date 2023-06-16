package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.listeners.IGameListener;
import games.GameType;
import players.PlayerFactory;
import players.mcts.BasicMCTSPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult;
import static evaluation.tournaments.RoundRobinTournament.TournamentMode.*;
import static java.util.stream.Collectors.toList;
import static utilities.Utils.getArg;


public class RoundRobinTournament extends AbstractTournament {
    private static boolean debug = false;
    public final TournamentMode tournamentMode;
    private final int gamesPerMatchUp;
    protected List<IGameListener> listeners = new ArrayList<>();
    public boolean verbose = true;
    double[] pointsPerPlayer;
    double[] pointsPerPlayerSquared;
    double[] rankPerPlayer;
    double[] rankPerPlayerSquared;
    int[] gamesPerPlayer;
    protected LinkedHashMap<Integer, Pair<Double, Double>> finalWinRanking; // contains index of agent in agents
    protected LinkedHashMap<Integer, Pair<Double, Double>> finalOrdinalRanking; // contains index of agent in agents
    LinkedList<Integer> agentIDs;
    private int matchUpsRun;
    private boolean randomGameParams;
    public final String name;

    private boolean listenersInitialized = false;

    public enum TournamentMode {
        SELF_PLAY,
        NO_SELF_PLAY,
        ONE_VS_ALL
    }

    /**
     * Main function, creates and runs the tournament with the given settings and players.
     */
    @SuppressWarnings({"ConstantConditions"})
    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            System.out.println(
                    "There are a number of possible arguments:\n" +
                            "\tgame=          A list of the games to be played. If there is more than one, then use a \n" +
                            "\t               pipe-delimited list, for example games=Uno|ColtExpress|Pandemic.\n" +
                            "\t               The default is 'all' to indicate that all games should be analysed.\n" +
                            "\t               Specifying all|-name1|-name2... will run all games except for name1, name2...\n" +
                            "\tnPlayers=      The total number of players in each game (the default is 'all') \n " +
                            "\t               A range can also be specified, for example 3-5. \n " +
                            "\t               Different player counts can be specified for each game in pipe-delimited format.\n" +
                            "\t               If 'all' is specified, then every possible playerCount for the game will be analysed.\n" +
                            "\tplayers=       The directory containing agent JSON files for the competing Players\n" +
                            "\t               If not specified, this defaults to very basic OSLA, RND, RHEA and MCTS players.\n" +
                            "\tmode=          exhaustive|random - defaults to exhaustive.\n" +
                            "\t               'exhaustive' will iterate exhaustively through every possible permutation: \n" +
                            "\t               every possible player in every possible position, and run a number of games equal to 'matchups'\n" +
                            "\t               for each. This can be excessive for a large number of players." +
                            "\t               'random' will have a random matchup, while ensuring no duplicates, and that all players get the\n" +
                            "\t               the same number of games in total.\n" +
                            "\t               If a focusPlayer is provided, then this is ignored.\n" +
                            "\tmatchups=      The total number of matchups to run if mode=random...\n" +
                            "\t               ...or the number of matchups to run per combination of players if mode=exhaustive\n" +
                            "\tdestDir=       The directory to which the results will be written. Defaults to 'metrics/out'.\n" +
                            "\t               If (and only if) this is being run for multiple games/player counts, then a subdirectory\n" +
                            "\t               will be created for each game, and then within that for  each player count combination.\n" +
                            "\taddTimestamp=  (Optional) If true (default is false), then the results will be written to a subdirectory of destDir.\n" +
                            "\t               This may be useful if you want to use the same destDir for multiple experiments.\n" +
                            "\tlistener=      The full class name of an IGameListener implementation. Or the location\n" +
                            "\t               of a json file from which a listener can be instantiated.\n" +
                            "\t               Defaults to evaluation.metrics.MetricsGameListener. \n" +
                            "\t               A pipe-delimited string can be provided to gather many types of statistics \n" +
                            "\t               from the same set of games.\n" +
                            "\tmetrics=       (Optional) The full class name of an IMetricsCollection implementation. " +
                            "\t               The recommended usage is to include these in the JSON file that defines the listener,\n" +
                            "\t               but this option is here for quick and dirty tests.\n" +
                            "\tfocusPlayer=   (Optional) A JSON file that defines the 'focus' of the tournament.\n" +
                            "\t               The 'focus' player will be present in every single game.\n" +
                            "\t               In this case 'matchups' defines the number of games to be run with the focusPlayer\n" +
                            "\t               in each position. The other positions will be filled randomly from players.\n" +
                            "\tgameParams=    (Optional) A JSON file from which the game parameters will be initialised.\n" +
                            "\tselfPlay=      (Optional) If true, then multiple copies of the same agent can be in one game.\n" +
                            "\t               Defaults to false\n" +
                            "\treportPeriod=  (Optional) For random mode execution only, after how many games played results are reported.\n" +
                            "\t               Defaults to the end of the tournament\n" +
                            "\trandomGameParams= (Optional) If specified, parameters for the game will be randomized for each game, and printed before the run.\n" +
                            "\toutput=        (Optional) If specified, the summary results will be written to a file with this name.\n"

            );
            return;
        }
        /* 1. Settings for the tournament */
        Map<GameType, int[]> gamesAndPlayerCounts = initialiseGamesAndPlayerCount(args);

        boolean selfPlay = getArg(args, "selfPlay", false);
        String mode = getArg(args, "mode", "random");
        int matchups = getArg(args, "matchups", 1);
        String playerDirectory = getArg(args, "players", "");
        String focusPlayer = getArg(args, "focusPlayer", "");

        String destDir = getArg(args, "destDir", "metrics/out");
        boolean addTimestamp = getArg(args, "addTimestamp", false);
        int reportPeriod = getArg(args, "reportPeriod", matchups);
        boolean verbose = getArg(args, "verbose", false);
        String resultsFile = getArg(args, "output", "");

        List<String> listenerClasses = new ArrayList<>(Arrays.asList(getArg(args, "listener", "evaluation.listeners.MetricsGameListener").split("\\|")));
        String metricsClass = getArg(args, "metrics", "evaluation.metrics.GameMetrics");

        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        if (!playerDirectory.equals("")) {
            agents.addAll(PlayerFactory.createPlayers(playerDirectory));
        } else {
            /* 2. Set up players */
            agents.add(new MCTSPlayer());
            agents.add(new BasicMCTSPlayer());
            agents.add(new RandomPlayer());
            agents.add(new RMHCPlayer());
            agents.add(new OSLAPlayer());
        }
        AbstractPlayer focus = null;
        if (!focusPlayer.equals("")) {
            focus = PlayerFactory.createPlayer(focusPlayer);
            agents.add(0, focus);  // convention is that they go first in the list of agents
        }

        String gameParams = getArg(args, "gameParams", "");
        if (!gameParams.equals("") && gamesAndPlayerCounts.keySet().size() > 1)
            throw new IllegalArgumentException("Cannot yet provide a gameParams argument if running multiple games");

        TournamentMode tournamentMode = selfPlay ? TournamentMode.SELF_PLAY : NO_SELF_PLAY;
        if (focus != null)
            tournamentMode = ONE_VS_ALL;

        String timeDir = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // Now we loop over each game and player count combination
        for (GameType gameType : gamesAndPlayerCounts.keySet()) {
            String gameName = gameType.name();
            //     timeDir.insert(0, gameName + "_");

            for (int playerCount : gamesAndPlayerCounts.get(gameType)) {
                System.out.printf("Game: %s, Players: %d\n", gameName, playerCount);
                String playersDir = playerCount + "-players";

                AbstractParameters params = gameParams.equals("") ? null : AbstractParameters.createFromFile(gameType, gameParams);

                RoundRobinTournament tournament = mode.equals("exhaustive") || tournamentMode == ONE_VS_ALL ?
                        new RoundRobinTournament(agents, gameType, playerCount, matchups, tournamentMode, params) :
                        new RandomRRTournament(agents, gameType, playerCount, tournamentMode, matchups, reportPeriod,
                                System.currentTimeMillis(), params);

                // Add listeners
                for (String listenerClass : listenerClasses) {
                    IGameListener gameTracker = IGameListener.createListener(listenerClass, metricsClass);
                    tournament.listeners.add(gameTracker);
                    List<String> directories = new ArrayList<>();
                    directories.add(destDir);
                    if (gamesAndPlayerCounts.size() > 1)
                        directories.add(gameName);
                    if (gamesAndPlayerCounts.get(gameType).length > 1)
                        directories.add(playersDir);
                    if (addTimestamp)
                        directories.add(timeDir);
                    gameTracker.setOutputDirectory(directories.toArray(new String[0]));
                }

                // run tournament
                tournament.verbose = verbose;
                tournament.resultsFile = resultsFile;
                tournament.randomGameParams = getArg(args, "randomGameParams", false);
                tournament.runTournament();
            }
        }


    }

    private static Map<GameType, int[]> initialiseGamesAndPlayerCount(String[] args) {
        List<String> tempGames = new ArrayList<>(Arrays.asList(getArg(args, "game", "all").split("\\|")));
        List<String> games = tempGames;
        if (tempGames.get(0).equals("all")) {
            games = Arrays.stream(GameType.values()).map(Enum::name).filter(name -> !tempGames.contains("-" + name)).collect(toList());
        }

        // This creates a <MinPlayer, MaxPlayer> Pair for each game#
        List<Pair<Integer, Integer>> nPlayers = Arrays.stream(getArg(args, "nPlayers", "all").split("\\|"))
                .map(str -> {
                    if (str.contains("-")) {
                        int hyphenIndex = str.indexOf("-");
                        return new Pair<>(Integer.valueOf(str.substring(0, hyphenIndex)), Integer.valueOf(str.substring(hyphenIndex + 1)));
                    } else if (str.equals("all")) {
                        return new Pair<>(-1, -1); // the next step will fill in the correct values
                    } else
                        return new Pair<>(Integer.valueOf(str), Integer.valueOf(str));
                }).collect(toList());
        // Then fill in the min/max player counts for the games that were specified as "all"
        // And repair min/max player counts that were specified incorrectly
        for (int i = 0; i < nPlayers.size(); i++) {
            GameType game = GameType.valueOf(games.get(i));
            if (nPlayers.get(i).a == -1) {
                nPlayers.set(i, new Pair<>(game.getMinPlayers(), game.getMaxPlayers()));
            }
            if (nPlayers.get(i).a < game.getMinPlayers())
                nPlayers.set(i, new Pair<>(game.getMinPlayers(), nPlayers.get(i).b));
            if (nPlayers.get(i).b > game.getMaxPlayers())
                nPlayers.set(i, new Pair<>(nPlayers.get(i).a, game.getMaxPlayers()));
        }

        // if only one game size was provided, then it applies to all games in the list
        if (games.size() == 1 && nPlayers.size() > 1) {
            for (int loop = 0; loop < nPlayers.size() - 1; loop++)
                games.add(games.get(0));
        }
        if (nPlayers.size() == 1 && games.size() > 1) {
            for (int loop = 0; loop < games.size() - 1; loop++)
                nPlayers.add(nPlayers.get(0));
        }
        if (nPlayers.size() > 1 && nPlayers.size() != games.size())
            throw new IllegalArgumentException("If specified, then nPlayers length must be one, or match the length of the games list");

        Map<GameType, int[]> gamesAndPlayerCounts = new LinkedHashMap<>();
        for (int i = 0; i < games.size(); i++) {
            GameType game = GameType.valueOf(games.get(i));
            int minPlayers = nPlayers.get(i).a;
            int maxPlayers = nPlayers.get(i).b;
            int[] playerCounts = new int[maxPlayers - minPlayers + 1];
            Arrays.setAll(playerCounts, n -> n + minPlayers);
            gamesAndPlayerCounts.put(game, playerCounts);
        }
        return gamesAndPlayerCounts;
    }


    /**
     * Create a round robin tournament, which plays all agents against all others.
     *
     * @param agents          - players for the tournament.
     * @param gameToPlay      - game to play in this tournament.
     * @param playersPerGame  - number of players per game.
     * @param gamesPerMatchUp - number of games for each combination of players.
     * @param mode            - SELF_PLAY, NO_SELF_PLAY, or ONE_VS_ALL
     */
    public RoundRobinTournament(List<? extends AbstractPlayer> agents, GameType gameToPlay, int playersPerGame,
                                int gamesPerMatchUp, TournamentMode mode, AbstractParameters gameParams) {
        super(agents, gameToPlay, playersPerGame, gameParams);
        if (mode == NO_SELF_PLAY && playersPerGame > this.agents.size()) {
            throw new IllegalArgumentException("Not enough agents to fill a match without self-play." +
                    "Either add more agents, reduce the number of players per game, or allow self-play.");
        }

        this.agentIDs = new LinkedList<>();
        for (int i = 0; i < this.agents.size(); i++)
            this.agentIDs.add(i);

        this.gamesPerMatchUp = gamesPerMatchUp;
        this.tournamentMode = mode;
        this.pointsPerPlayer = new double[agents.size()];
        this.pointsPerPlayerSquared = new double[agents.size()];
        this.rankPerPlayer = new double[agents.size()];
        this.rankPerPlayerSquared = new double[agents.size()];
        this.gamesPerPlayer = new int[agents.size()];
        this.name = String.format("Game: %s, Players: %d, GamesPerMatchup: %d, Mode: %s", gameToPlay.name(), playersPerGame, gamesPerMatchUp, mode.name());
    }


    /**
     * Runs the round robin tournament.
     */
    @Override
    public void runTournament() {
        if (verbose)
            System.out.println("Playing " + games.getGameType().name());
        LinkedList<Integer> matchUp = new LinkedList<>();
        createAndRunMatchUp(matchUp);
        reportResults();

        for (IGameListener listener : listeners)
            listener.allGamesFinished();
    }


    public int getWinnerIndex() {
        if (finalWinRanking == null || finalWinRanking.isEmpty())
            throw new UnsupportedOperationException("Cannot get winner before results have been calculated");

        // The winner is the first key in finalRanking
        for (Integer key : finalWinRanking.keySet()) {
            return key;
        }
        throw new AssertionError("Should not be reachable");
    }

    public AbstractPlayer getWinner() {
        return agents.get(getWinnerIndex());
    }

    /**
     * Recursively creates one combination of players and evaluates it.
     *
     * @param matchUp - current combination of players, updated recursively.
     */
    public void createAndRunMatchUp(LinkedList<Integer> matchUp) {
        if (tournamentMode == ONE_VS_ALL) {
            // In this case agents.get(0) must always play
            Random rnd = new Random(System.currentTimeMillis());
            int nPlayers = playersPerGame;
            List<Integer> randomAgentOrder = new ArrayList<>(this.agentIDs);
            randomAgentOrder.remove(Integer.valueOf(0));
            for (int p = 0; p < nPlayers; p++) {
                // we put the focus player at each position (p) in turn
                for (int m = 0; m < this.gamesPerMatchUp; m++) {
                    Collections.shuffle(randomAgentOrder, rnd);
                    List<Integer> matchup = new ArrayList<>(nPlayers);
                    for (int j = 0; j < nPlayers; j++) {
                        if (j == p)
                            matchup.add(0); // focus player
                        else {
                            matchup.add(randomAgentOrder.get(j % randomAgentOrder.size()));
                        }
                    }
                    evaluateMatchUp(matchup, 1);
                }
            }
        } else {
            // in this case we are in exhaustive mode, so we recursively construct all possible combinations of players
            if (matchUp.size() == playersPerGame) {
                evaluateMatchUp(matchUp);
            } else {
                for (Integer agentID : this.agentIDs) {
                    if (tournamentMode == SELF_PLAY || !matchUp.contains(agentID)) {
                        matchUp.add(agentID);
                        createAndRunMatchUp(matchUp);
                        matchUp.remove(agentID);
                    }
                }
            }
        }
    }

    protected void evaluateMatchUp(List<Integer> agentIDs) {
        evaluateMatchUp(agentIDs, gamesPerMatchUp);
    }

    /**
     * Evaluates one combination of players.
     *
     * @param agentIDs - IDs of agents participating in this run.
     */
    protected void evaluateMatchUp(List<Integer> agentIDs, int nGames) {
        if (debug)
            System.out.printf("Evaluate %s at %tT%n", agentIDs.toString(), System.currentTimeMillis());
        LinkedList<AbstractPlayer> matchUpPlayers = new LinkedList<>();

        for (int agentID : agentIDs)
            matchUpPlayers.add(this.agents.get(agentID));

        if (verbose) {
            StringBuffer sb = new StringBuffer();
            sb.append("[");
            for (int agentID : agentIDs)
                sb.append(this.agents.get(agentID).toString()).append(",");
            sb.setCharAt(sb.length() - 1, ']');
            System.out.println(sb);
        }


        for (IGameListener listener : listeners) {
            games.addListener(listener);
        }


        // Run the game N = gamesPerMatchUp times with these players
        long currentSeed = games.getGameState().getGameParameters().getRandomSeed();
        for (int i = 0; i < nGames; i++) {

            if (randomGameParams)
                games.getGameState().getGameParameters().randomize();

            games.reset(matchUpPlayers, currentSeed + i + 1);
            if (!listenersInitialized) {
                for (IGameListener gameTracker : listeners) {
                    gameTracker.init(games);
                }
                listenersInitialized = true;
            }

            // Randomize parameters
            if (randomGameParams) {
                games.getGameState().getGameParameters().randomize();
                System.out.println("Game parameters: " + games.getGameState().getGameParameters());
            }
            games.run();  // Always running tournaments without visuals
            GameResult[] results = games.getGameState().getPlayerResults();

            int numDraws = 0;
            for (int j = 0; j < matchUpPlayers.size(); j++) {
                int ordinalPos = games.getGameState().getOrdinalPosition(j);
                rankPerPlayer[agentIDs.get(j)] += ordinalPos;
                gamesPerPlayer[agentIDs.get(j)] += 1;
                rankPerPlayerSquared[agentIDs.get(j)] += ordinalPos * ordinalPos;
                if (results[j] == GameResult.WIN_GAME) {
                    pointsPerPlayer[agentIDs.get(j)] += 1;
                    pointsPerPlayerSquared[agentIDs.get(j)] += 1;
                }
                if (results[j] == GameResult.DRAW_GAME)
                    numDraws++;
            }

            if (numDraws > 0) {
                double pointsPerDraw = 1.0 / numDraws;
                for (int j = 0; j < matchUpPlayers.size(); j++) {
                    if (results[j] == GameResult.DRAW_GAME) pointsPerPlayer[agentIDs.get(j)] += pointsPerDraw;
                    if (results[j] == GameResult.DRAW_GAME)
                        pointsPerPlayerSquared[agentIDs.get(j)] += pointsPerDraw * pointsPerDraw;
                }
            }

            if (verbose) {
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                for (int j = 0; j < matchUpPlayers.size(); j++)
                    sb.append(results[j]).append(",");
                sb.setCharAt(sb.length() - 1, ']');
                System.out.println(sb);
            }

        }
        games.clearListeners();
        matchUpsRun++;
    }


    protected void calculateFinalResults() {
        finalWinRanking = new LinkedHashMap<>();
        finalOrdinalRanking = new LinkedHashMap<>();
        for (int i = 0; i < this.agents.size(); i++) {
            // We calculate the standard deviation, and hence the standard error on the mean value
            // (using a normal approximation, which is valid for large N)
            double stdDev = pointsPerPlayerSquared[i] / gamesPerPlayer[i] - (pointsPerPlayer[i] / gamesPerPlayer[i])
                    * (pointsPerPlayer[i] / gamesPerPlayer[i]);
            finalWinRanking.put(i, new Pair<>(pointsPerPlayer[i] / gamesPerPlayer[i], stdDev / Math.sqrt(gamesPerPlayer[i])));
            stdDev = rankPerPlayerSquared[i] / gamesPerPlayer[i] - (rankPerPlayer[i] / gamesPerPlayer[i]) * (rankPerPlayer[i] / gamesPerPlayer[i]);
            finalOrdinalRanking.put(i, new Pair<>(rankPerPlayer[i] / gamesPerPlayer[i], stdDev / Math.sqrt(gamesPerPlayer[i])));
        }
        // Sort by points.
        finalWinRanking = finalWinRanking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((o1, o2) -> o2.a.compareTo(o1.a)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
                        LinkedHashMap::new));
        finalOrdinalRanking = finalOrdinalRanking.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((o1, o2) -> o2.a.compareTo(o1.a)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    protected void reportResults() {
        calculateFinalResults();
        int gameCounter = (gamesPerMatchUp * matchUpsRun);
        boolean toFile = !resultsFile.equals("");
        ArrayList<String> dataDump = new ArrayList<>();
        dataDump.add(name);

        // To console
        if (verbose)
            System.out.printf("============= %s - %d games played ============= \n", games.getGameType().name(), gameCounter);
        for (int i = 0; i < this.agents.size(); i++) {
            String str = String.format("%s got %.2f points. ", agents.get(i), pointsPerPlayer[i]);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            str = String.format("%s won %.1f%% of the %d games of the tournament. ",
                    agents.get(i), 100.0 * pointsPerPlayer[i] / gameCounter, gameCounter);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);

            str = String.format("%s won %.1f%% of the %d games it played during the tournament.\n",
                    agents.get(i), 100.0 * pointsPerPlayer[i] / gamesPerPlayer[i], gamesPerPlayer[i]);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);
        }

        String str = "---- Ranking ---- \n";
        if (toFile) dataDump.add(str);
        if (verbose) System.out.print(str);

        for (Integer i : finalWinRanking.keySet()) {
            str = String.format("%s: Win rate %.2f +/- %.2f\tMean Ordinal %.2f +/- %.2f\n",
                    agents.get(i).toString(),
                    finalWinRanking.get(i).a, finalWinRanking.get(i).b,
                    finalOrdinalRanking.get(i).a, finalOrdinalRanking.get(i).b);
            if (toFile) dataDump.add(str);
            if (verbose) System.out.print(str);
        }

        // To file
        if (toFile) {
            try {
                FileWriter writer = new FileWriter(resultsFile, true);
                for (String line : dataDump)
                    writer.write(line);
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public double getWinRate(int agentID) {
        return finalWinRanking.get(agentID).a;
    }

    public double getWinStdErr(int agentID) {
        return finalWinRanking.get(agentID).b;
    }

    public double getOrdinalRank(int agentID) {
        return finalOrdinalRanking.get(agentID).a;
    }

    public double getOrdinalStdErr(int agentID) {
        return finalOrdinalRanking.get(agentID).b;
    }

    public List<IGameListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<IGameListener> listeners) {
        this.listeners = listeners;
    }
}
