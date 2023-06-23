package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.AbstractTournament.TournamentMode;
import evaluation.tournaments.RandomRRTournament;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.PlayerFactory;
import players.mcts.BasicMCTSPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.text.SimpleDateFormat;
import java.util.*;

import static evaluation.tournaments.AbstractTournament.TournamentMode.*;
import static java.util.stream.Collectors.toList;
import static utilities.Utils.getArg;


public class RunGames {

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
                            "\t               pipe-delimited list, for example game=Uno|ColtExpress|Pandemic.\n" +
                            "\t               The default is 'all' to indicate that all games should be analysed.\n" +
                            "\t               Specifying all|-name1|-name2... will run all games except for name1, name2...\n" +
                            "\tnPlayers=      The total number of players in each game (the default is 'all') \n " +
                            "\t               A range can also be specified, for example 3-5. \n " +
                            "\t               Different player counts can be specified for each game in pipe-delimited format.\n" +
                            "\t               If 'all' is specified, then every possible playerCount for the game will be analysed.\n" +
                            "\tverbose=       If true, then the result of each game is reported. Default is false.\n"+
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

        TournamentMode tournamentMode = selfPlay ? SELF_PLAY : NO_SELF_PLAY;
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
                    tournament.addListener(gameTracker);
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
                tournament.setVerbose(verbose);
                tournament.setResultsFile(resultsFile);
                tournament.setRandomGameParams(getArg(args, "randomGameParams", false));
                tournament.runTournament();
            }
        }


    }

    private static Map<GameType, int[]> initialiseGamesAndPlayerCount(String[] args) {
        List<String> tempGames = new ArrayList<>(Arrays.asList(getArg(args, "game", "all").split("\\|")));
        List<String> games = tempGames;
        if (tempGames.get(0).equals("all")) {
            tempGames.add("-GameTemplate"); // so that  we always remove this one
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
        // if only one game size was provided, then it applies to all games in the list
        // or vice versa
        if (games.size() == 1 && nPlayers.size() > 1) {
            for (int loop = 0; loop < nPlayers.size() - 1; loop++)
                games.add(games.get(0));
        }
        if (nPlayers.size() == 1 && games.size() > 1) {
            for (int loop = 0; loop < games.size() - 1; loop++)
                nPlayers.add(nPlayers.get(0));
        }
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

        if (nPlayers.size() > 1 && nPlayers.size() != games.size())
            throw new IllegalArgumentException("If specified, then nPlayers length must be one, or match the length of the games list");

        Map<GameType, int[]> gamesAndPlayerCounts = new LinkedHashMap<>();
        for (int i = 0; i < games.size(); i++) {
            GameType game = GameType.valueOf(games.get(i));
            int minPlayers = nPlayers.get(i).a;
            int maxPlayers = nPlayers.get(i).b;
            if (maxPlayers < minPlayers)
                continue;  // in this case the game does not support the desired player count
            int[] playerCounts = new int[maxPlayers - minPlayers + 1];
            Arrays.setAll(playerCounts, n -> n + minPlayers);
            gamesAndPlayerCounts.put(game, playerCounts);
        }
        return gamesAndPlayerCounts;
    }

}
