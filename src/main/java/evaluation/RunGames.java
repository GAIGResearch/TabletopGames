package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.IGameRunner;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import evaluation.tournaments.SkillGrid;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.PlayerFactory;
import players.PlayerType;
import players.basicMCTS.BasicMCTSPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static evaluation.RunArg.*;
import static java.util.stream.Collectors.toList;


public class RunGames implements IGameRunner {

    // Config
    Map<RunArg, Object> config = new HashMap<>();

    // Vars for running
    Map<GameType, int[]> gamesAndPlayerCounts;
    private LinkedList<AbstractPlayer> agents;
    private String timeDir;

    /**
     * Main function, creates and runs the tournament with the given settings and players.
     */
    @SuppressWarnings({"ConstantConditions"})
    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            RunArg.printHelp(Usage.RunGames);
            return;
        }

        /* 1. Settings for the tournament */
        RunGames runGames = new RunGames();
        runGames.config = parseConfig(args, Collections.singletonList(Usage.RunGames));

        runGames.initialiseGamesAndPlayerCount();
        if (!runGames.config.get(RunArg.gameParams).equals("") && runGames.gamesAndPlayerCounts.keySet().size() > 1)
            throw new IllegalArgumentException("Cannot yet provide a gameParams argument if running multiple games");

        // 2. Setup
        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        if (!runGames.config.get(playerDirectory).equals("")) {
            agents.addAll(PlayerFactory.createPlayers((String) runGames.config.get(playerDirectory)));
        } else {
       //     agents.add(new MCTSPlayer());
            agents.add(new BasicMCTSPlayer());
            agents.add(new RandomPlayer());
            agents.add(new RMHCPlayer());
            agents.add(new OSLAPlayer());
        }
        runGames.agents = agents;

        if (!runGames.config.get(focusPlayer).equals("")) {
            // if a focus Player is provided, then this override some other settings
            runGames.config.put(mode, "onevsall");
            AbstractPlayer fp = PlayerFactory.createPlayer((String) runGames.config.get(focusPlayer));
            agents.add(0, fp);  // convention is that they go first in the list of agents
        }

        runGames.timeDir = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // 3. Run!
        if (runGames.config.get(mode).equals("sequential")) {
            SkillGrid main = new SkillGrid(agents, runGames.config);
            main.run();
        } else {
            runGames.run();
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

                RoundRobinTournament tournament = new RoundRobinTournament(agents, gameType, playerCount, params, config);

                // Add listeners
                //noinspection unchecked
                for (String listenerClass : ((List<String>) config.get(listener))) {
                    try {
                        IGameListener gameTracker = IGameListener.createListener(listenerClass);
                    tournament.addListener(gameTracker);
                    String outputDir = (String) config.get(destDir);
                    List<String> directories = new ArrayList<>(Arrays.asList(outputDir.split(Pattern.quote(File.separator))));
                    if (gamesAndPlayerCounts.size() > 1)
                        directories.add(gameName);
                    if (gamesAndPlayerCounts.get(gameType).length > 1)
                        directories.add(playersDir);
                    if ((boolean) config.get(addTimeStamp))
                        directories.add(timeDir);
                    gameTracker.setOutputDirectory(directories.toArray(new String[0]));
                    } catch (IllegalArgumentException e) {
                        System.out.println("Error creating listener: " + e.getMessage());
                        // this is not a problem as such, we'll still report win rate information which may be all the user wants
                    }
                }

                // run tournament
                tournament.run();
            }
        }
    }

    private void initialiseGamesAndPlayerCount() {
        String gameArg = config.get(RunArg.game).toString();
        String playerRange = config.get(RunArg.playerRange).toString();
        int np = (int) config.get(RunArg.nPlayers);
        if (np > 0)
            playerRange = String.valueOf(np);
        List<String> tempGames = new ArrayList<>(Arrays.asList(gameArg.split("\\|")));
        List<String> games = tempGames;
        if (tempGames.get(0).equals("all")) {
            tempGames.add("-GameTemplate"); // so that  we always remove this one
            games = Arrays.stream(GameType.values()).map(Enum::name).filter(name -> !tempGames.contains("-" + name)).collect(toList());
        }

        // This creates a <MinPlayer, MaxPlayer> Pair for each game#
        List<Pair<Integer, Integer>> nPlayers = Arrays.stream(playerRange.split("\\|"))
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
            int max = game.getMaxPlayers();

            // Cap max number of players to those available in the framework if no player directory specified
            // (in which case the framework will use 1 of each default players)
            if (config.get(playerDirectory).equals("") && max > PlayerType.values().length - 2) {
                max = PlayerType.values().length - 2;  // Ignore the 2 human players (console, GUI)
            }

            if (nPlayers.get(i).a == -1) {
                nPlayers.set(i, new Pair<>(game.getMinPlayers(), max));
            }
            if (nPlayers.get(i).a < game.getMinPlayers())
                nPlayers.set(i, new Pair<>(game.getMinPlayers(), nPlayers.get(i).b));
            if (nPlayers.get(i).b > max)
                nPlayers.set(i, new Pair<>(nPlayers.get(i).a, max));
        }

        if (nPlayers.size() > 1 && nPlayers.size() != games.size())
            throw new IllegalArgumentException("If specified, then nPlayers length must be one, or match the length of the games list");

        gamesAndPlayerCounts = new LinkedHashMap<>();
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
    }
}
