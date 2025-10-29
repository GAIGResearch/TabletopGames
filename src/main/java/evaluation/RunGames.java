package evaluation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.interfaces.IGameRunner;
import evaluation.listeners.IGameListener;
import evaluation.tournaments.RoundRobinTournament;
import evaluation.tournaments.SkillGrid;
import games.GameType;
import org.json.simple.parser.ParseException;
import players.PlayerFactory;
import players.PlayerType;
import players.basicMCTS.BasicMCTSPlayer;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static evaluation.RunArg.*;
import static java.util.stream.Collectors.toList;

/**
 * RunGames
 * ----------
 * Main tournament runner for the TAG framework.
 * Supports JSON-based configuration of players, parameters, and tournament modes.
 *
 * Updated by: Ali Askari (MSc Artificial Intelligence)
 * Date: 29/10/2025
 * Change: Added support for JSON-specified players (e.g., OppAwareMCTSPlayer)
 */
public class RunGames implements IGameRunner {

    // Parsed configuration
    Map<RunArg, Object> config = new HashMap<>();

    // Tournament variables
    Map<GameType, int[]> gamesAndPlayerCounts;
    private LinkedList<AbstractPlayer> agents;
    private String timeDir;

    /**
     * Entry point for launching a tournament or batch of games.
     */
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            RunArg.printHelp(Usage.RunGames);
            return;
        }

        // 1. Parse configuration from CLI/JSON
        RunGames runGames = new RunGames();
        runGames.config = parseConfig(args, Collections.singletonList(Usage.RunGames));
        runGames.initialiseGamesAndPlayerCount();

        if (!runGames.config.get(RunArg.gameParams).equals("") &&
                runGames.gamesAndPlayerCounts.keySet().size() > 1)
            throw new IllegalArgumentException("Cannot provide a gameParams argument for multiple games");

        // 2. Setup player list
        LinkedList<AbstractPlayer> agents = new LinkedList<>();

        // Check if a player list is defined in the JSON config
        List<String> playerClasses = (List<String>) runGames.config.get(RunArg.players);
        if (playerClasses != null && !playerClasses.isEmpty()) {
            System.out.println("Loading players from JSON configuration:");
            for (String className : playerClasses) {
                try {
                    AbstractPlayer player;
                    try {
                        // Try to load using reflection (fully qualified class name)
                        Class<?> clazz = Class.forName(className);
                        player = (AbstractPlayer) clazz.getDeclaredConstructor().newInstance();
                    } catch (ClassNotFoundException e) {
                        // Fallback to PlayerFactory default behaviour (short names)
                        player = PlayerFactory.createPlayer(className);
                    }
                    agents.add(player);
                    System.out.println("✅ Loaded: " + className);

                    agents.add(player);
                    System.out.println("  ✅ Loaded: " + className);
                } catch (Exception e) {
                    System.err.println("⚠️  Could not load player: " + className);
                    e.printStackTrace();
                }
            }
        } else {
            // fallback default players
            System.out.println("No player list provided — using default agent set.");
            agents.add(new BasicMCTSPlayer());
            agents.add(new RandomPlayer());
            agents.add(new RMHCPlayer());
            agents.add(new OSLAPlayer());
        }
        runGames.agents = agents;

        // 3. Focus player logic (e.g. for one-vs-all setups)
        if (!runGames.config.get(focusPlayer).equals("")) {
            runGames.config.put(mode, "onevsall");
            AbstractPlayer fp = PlayerFactory.createPlayer((String) runGames.config.get(focusPlayer));
            agents.add(0, fp); // convention: goes first
        }

        // 4. Timestamped directory for results
        runGames.timeDir = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

        // 5. Run tournament
        if (runGames.config.get(mode).equals("sequential")) {
            SkillGrid main = new SkillGrid(agents, runGames.config);
            main.run();
        } else {
            runGames.run();
        }
    }

    /**
     * Core run method — executes the configured tournaments.
     */
    @Override
    public void run() {
        for (GameType gameType : gamesAndPlayerCounts.keySet()) {
            String gameName = gameType.name();

            for (int playerCount : gamesAndPlayerCounts.get(gameType)) {
                System.out.printf("Game: %s, Players: %d\n", gameName, playerCount);

                String playersDir = playerCount + "-players";
                AbstractParameters params =
                        config.get(gameParams).equals("") ? null :
                                AbstractParameters.createFromFile(gameType, (String) config.get(gameParams));

                RoundRobinTournament tournament = new RoundRobinTournament(agents, gameType, playerCount, params, config);

                // Attach tournament listeners
                @SuppressWarnings("unchecked")
                List<String> listeners = (List<String>) config.get(listener);
                for (String listenerClass : listeners) {
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
                    }
                }

                // Run the actual tournament
                tournament.run();
            }
        }
    }

    /**
     * Prepares the (gameType, playerCount) mapping for tournaments.
     */
    private void initialiseGamesAndPlayerCount() {
        String gameArg = config.get(RunArg.game).toString();
        String playerRange = config.get(RunArg.playerRange).toString();
        int np = (int) config.get(RunArg.nPlayers);
        if (np > 0)
            playerRange = String.valueOf(np);

        List<String> tempGames = new ArrayList<>(Arrays.asList(gameArg.split("\\|")));
        List<String> games = tempGames;
        if (tempGames.get(0).equals("all")) {
            tempGames.add("-GameTemplate");
            games = Arrays.stream(GameType.values())
                    .map(Enum::name)
                    .filter(name -> !tempGames.contains("-" + name))
                    .collect(toList());
        }

        // Parse player ranges for each game
        List<Pair<Integer, Integer>> nPlayers = Arrays.stream(playerRange.split("\\|"))
                .map(str -> {
                    if (str.contains("-")) {
                        int hyphenIndex = str.indexOf("-");
                        return new Pair<>(Integer.valueOf(str.substring(0, hyphenIndex)),
                                Integer.valueOf(str.substring(hyphenIndex + 1)));
                    } else if (str.equals("all")) {
                        return new Pair<>(-1, -1);
                    } else {
                        return new Pair<>(Integer.valueOf(str), Integer.valueOf(str));
                    }
                }).collect(toList());

        if (games.size() == 1 && nPlayers.size() > 1) {
            for (int i = 0; i < nPlayers.size() - 1; i++)
                games.add(games.get(0));
        }
        if (nPlayers.size() == 1 && games.size() > 1) {
            for (int i = 0; i < games.size() - 1; i++)
                nPlayers.add(nPlayers.get(0));
        }

        // Validate player ranges per game
        for (int i = 0; i < nPlayers.size(); i++) {
            GameType game = GameType.valueOf(games.get(i));
            int max = game.getMaxPlayers();

            if (config.get(playerDirectory).equals("") && max > PlayerType.values().length - 2)
                max = PlayerType.values().length - 2;

            if (nPlayers.get(i).a == -1)
                nPlayers.set(i, new Pair<>(game.getMinPlayers(), max));

            if (nPlayers.get(i).a < game.getMinPlayers())
                nPlayers.set(i, new Pair<>(game.getMinPlayers(), nPlayers.get(i).b));
            if (nPlayers.get(i).b > max)
                nPlayers.set(i, new Pair<>(nPlayers.get(i).a, max));
        }

        if (nPlayers.size() > 1 && nPlayers.size() != games.size())
            throw new IllegalArgumentException("nPlayers list must match games list length");

        gamesAndPlayerCounts = new LinkedHashMap<>();
        for (int i = 0; i < games.size(); i++) {
            GameType game = GameType.valueOf(games.get(i));
            int minPlayers = nPlayers.get(i).a;
            int maxPlayers = nPlayers.get(i).b;
            if (maxPlayers < minPlayers)
                continue;

            int[] playerCounts = new int[maxPlayers - minPlayers + 1];
            Arrays.setAll(playerCounts, n -> n + minPlayers);
            gamesAndPlayerCounts.put(game, playerCounts);
        }
    }
}
