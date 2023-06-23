package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.IGameRunner;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.AbstractTournament;
import evaluation.tournaments.RandomRRTournament;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.PlayerFactory;
import players.mcts.BasicMCTSPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static evaluation.RunGames.RunArg.*;
import static evaluation.tournaments.AbstractTournament.TournamentMode.*;
import static java.util.stream.Collectors.toList;
import static utilities.Utils.getArg;


public class RunGames implements IGameRunner {

    enum RunArg {
        config ("The location of a JSON file from which to read the configuration. \n" +
                "\t If this is specified, then all other arguments are ignored.", ""),
        game ("A list of the games to be played. If there is more than one, then use a \n" +
                "\t pipe-delimited list, for example game=Uno|ColtExpress|Pandemic.\n" +
                "\t The default is 'all' to indicate that all games should be analysed.\n" +
                "\t Specifying all|-name1|-name2... will run all games except for name1, name2...", "all"),
        nPlayers ("The total number of players in each game (the default is 'all') \n " +
                "\t A range can also be specified, for example 3-5. \n " +
                "\t Different player counts can be specified for each game in pipe-delimited format.\n" +
                "\t If 'all' is specified, then every possible playerCount for the game will be analysed.", "all"),
        verbose ("If true, then the result of each game is reported. Default is false.", false),
        playerDirectory ("The directory containing agent JSON files for the competing Players\n" +
                "\t If not specified, this defaults to very basic OSLA, RND, RHEA and MCTS players.", ""),
        mode ("exhaustive|random - defaults to exhaustive.\n" +
                "\t 'exhaustive' will iterate exhaustively through every possible permutation: \n" +
                "\t every possible player in every possible position, and run a number of games equal to 'matchups'\n" +
                "\t for each. This can be excessive for a large number of players." +
                "\t 'random' will have a random matchup, while ensuring no duplicates, and that all players get the\n" +
                "\t the same number of games in total.\n" +
                "\t If a focusPlayer is provided, then this is ignored.", "random"),
        matchups ("The total number of matchups to run if mode=random...\n" +
                "\t ...or the number of matchups to run per combination of players if mode=exhaustive", 1),
        destDir ("The directory to which the results will be written. Defaults to 'metrics/out'.\n" +
                "\t If (and only if) this is being run for multiple games/player counts, then a subdirectory\n" +
                "\t will be created for each game, and then within that for  each player count combination.", "metrics/out"),
        addTimeStamp ("(Optional) If true (default is false), then the results will be written to a subdirectory of destDir.\n" +
                "\t This may be useful if you want to use the same destDir for multiple experiments.", false),
        listener ("The full class name of an IGameListener implementation. Or the location\n" +
                "\t of a json file from which a listener can be instantiated.\n" +
                "\t Defaults to evaluation.metrics.MetricsGameListener. \n" +
                "\t A pipe-delimited string can be provided to gather many types of statistics \n" +
                "\t from the same set of games.", "evaluation.listeners.MetricsGameListener"),
        metrics ("(Optional) The full class name of an IMetricsCollection implementation. " +
                "\t The recommended usage is to include these in the JSON file that defines the listener,\n" +
                "\t but this option is here for quick and dirty tests.", "evaluation.metrics.GameMetrics"),
        focusPlayer ("(Optional) A JSON file that defines the 'focus' of the tournament.\n" +
                "\t The 'focus' player will be present in every single game.\n" +
                "\t In this case 'matchups' defines the number of games to be run with the focusPlayer\n" +
                "\t in each position. The other positions will be filled randomly from players.", ""),
        gameParams ("(Optional) A JSON file from which the game parameters will be initialised.", ""),
        selfPlay ("(Optional) If true, then multiple copies of the same agent can be in one game.\n" +
                "\t Defaults to false", false),
        reportPeriod ("(Optional) For random mode execution only, after how many games played results are reported.\n" +
                "\t Defaults to the end of the tournament (-1)", -1),
        randomGameParams ("(Optional) If specified, parameters for the game will be randomized for each game, and printed before the run.", false),
        output ("(Optional) If specified, the summary results will be written to a file with this name.", "");

        public final String helpText;
        public final Object defaultValue;
        public Object value;
        RunArg(String helpText, Object defaultValue) {
            this.helpText = helpText;
            this.defaultValue = defaultValue;
        }
        public Object parse(Object args) {
            value = getArg(args, name(), defaultValue);
            if (this == listener) {
                value = new ArrayList<>(Arrays.asList(((String)value).split("\\|")));
            }
            return value;
        }
    }

    // Config
    Map<RunArg, Object> config = new HashMap<>();

    // Vars for running
    Map<GameType, int[]> gamesAndPlayerCounts;
    private LinkedList<AbstractPlayer> agents;
    private AbstractPlayer focus;
    private AbstractTournament.TournamentMode tournamentMode;
    private String timeDir;

    /**
     * Main function, creates and runs the tournament with the given settings and players.
     */
    @SuppressWarnings({"ConstantConditions"})
    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            System.out.println("There are a number of possible arguments:");
            for (RunArg arg : RunArg.values()) {
                System.out.println("\t" + arg.name() + "= " + arg.helpText + "\n");
            }
            return;
        }

        /* 1. Settings for the tournament */
        RunGames runGames = new RunGames();

        String setupFile = getArg(args, "config", "");
        if (!setupFile.equals("")) {
            // Read from file instead
            try {
                FileReader reader = new FileReader(setupFile);
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                parseConfig(runGames, json);
            } catch (FileNotFoundException ignored) {
                parseConfig(runGames, args);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            parseConfig(runGames, args);
        }
        if (!runGames.config.get(RunArg.gameParams).equals("") && runGames.gamesAndPlayerCounts.keySet().size() > 1)
            throw new IllegalArgumentException("Cannot yet provide a gameParams argument if running multiple games");

        // 2. Setup

        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        if (!runGames.config.get(playerDirectory).equals("")) {
            agents.addAll(PlayerFactory.createPlayers((String) runGames.config.get(playerDirectory)));
        } else {
            agents.add(new MCTSPlayer());
            agents.add(new BasicMCTSPlayer());
            agents.add(new RandomPlayer());
            agents.add(new RMHCPlayer());
            agents.add(new OSLAPlayer());
        }
        runGames.agents = agents;

        runGames.focus = null;
        if (!runGames.config.get(focusPlayer).equals("")) {
            runGames.focus = PlayerFactory.createPlayer((String) runGames.config.get(focusPlayer));
            agents.add(0, runGames.focus);  // convention is that they go first in the list of agents
        }

        runGames.tournamentMode = ((boolean)runGames.config.get(selfPlay)) ? SELF_PLAY : NO_SELF_PLAY;
        if (runGames.focus != null)
            runGames.tournamentMode = ONE_VS_ALL;

        runGames.timeDir = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // 3. Run!
        runGames.run();
    }

    private static void parseConfig(RunGames runGames, Object args) {
        runGames.gamesAndPlayerCounts = initialiseGamesAndPlayerCount((String) game.parse(args), (String) nPlayers.parse(args));
        for (RunArg arg : RunArg.values()) {
            if (arg == RunArg.config) continue;
            runGames.config.put(arg, arg.parse(args));
        }
    }

    @Override
    public void run() {
        // Now we loop over each game and player count combination
        for (GameType gameType : gamesAndPlayerCounts.keySet()) {
            String gameName = gameType.name();
            //     timeDir.insert(0, gameName + "_");

            for (int playerCount : gamesAndPlayerCounts.get(gameType)) {
                System.out.printf("Game: %s, Players: %d\n", gameName, playerCount);
                String playersDir = playerCount + "-players";

                AbstractParameters params = config.get(gameParams).equals("") ? null : AbstractParameters.createFromFile(gameType, (String) config.get(gameParams));

                RoundRobinTournament tournament = config.get(mode).equals("exhaustive") || tournamentMode == ONE_VS_ALL ?
                        new RoundRobinTournament(agents, gameType, playerCount, (int)config.get(matchups), tournamentMode, params, (String) config.get(destDir), timeDir) :
                        new RandomRRTournament(agents, gameType, playerCount, tournamentMode, (int)config.get(matchups), (int)config.get(reportPeriod),
                                System.currentTimeMillis(), params, (String) config.get(destDir), timeDir);

                // Add listeners
                //noinspection unchecked
                for (String listenerClass : ((List<String>)config.get(listener))) {
                    IGameListener gameTracker = IGameListener.createListener(listenerClass, (String)config.get(metrics));
                    tournament.addListener(gameTracker);
                    List<String> directories = new ArrayList<>();
                    directories.add((String)config.get(destDir));
                    if (gamesAndPlayerCounts.size() > 1)
                        directories.add(gameName);
                    if (gamesAndPlayerCounts.get(gameType).length > 1)
                        directories.add(playersDir);
                    if ((boolean)config.get(addTimeStamp))
                        directories.add(timeDir);
                    gameTracker.setOutputDirectory(directories.toArray(new String[0]));
                }

                // run tournament
                tournament.setVerbose((boolean)config.get(verbose));
                tournament.setResultsFile((String)config.get(output));
                tournament.setRandomGameParams((boolean)config.get(randomGameParams));
                tournament.run();
            }
        }
    }

    private static Map<GameType, int[]> initialiseGamesAndPlayerCount(String gameArg, String nPlayersArg) {
        List<String> tempGames = new ArrayList<>(Arrays.asList(gameArg.split("\\|")));
        List<String> games = tempGames;
        if (tempGames.get(0).equals("all")) {
            games = Arrays.stream(GameType.values()).map(Enum::name).filter(name -> !tempGames.contains("-" + name)).collect(toList());
        }

        // This creates a <MinPlayer, MaxPlayer> Pair for each game#
        List<Pair<Integer, Integer>> nPlayers = Arrays.stream(nPlayersArg.split("\\|"))
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
}
