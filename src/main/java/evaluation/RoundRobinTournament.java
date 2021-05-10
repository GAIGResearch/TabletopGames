package evaluation;

import core.AbstractPlayer;
import core.Game;
import games.GameType;
import players.PlayerFactory;
import players.mcts.BasicMCTSPlayer;
import players.simple.OSLAPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import utilities.*;

import java.io.File;
import java.util.*;

import static utilities.Utils.*;


public class RoundRobinTournament extends AbstractTournament {
    int[] pointsPerPlayer;
    LinkedList<Integer> agentIDs;
    private final int gamesPerMatchUp;
    private int matchUpsRun;
    public final boolean selfPlay;
    private int gameCounter;
    private FileStatsLogger dataLogger;

    /**
     * Main function, creates an runs the tournament with the given settings and players.
     */
    @SuppressWarnings({"UnnecessaryLocalVariable", "ConstantConditions"})
    public static void main(String[] args) {
        /* 1. Settings for the tournament */
        GameType gameToPlay = GameType.valueOf(getArg(args, "game", "Uno"));
        int nPlayersTotal = 4;
        int nPlayersPerGame = getArg(args, "players", 2);
        int nGamesPerMatchUp = getArg(args, "gamePerMatchup", 1);
        boolean selfPlay = getArg(args, "selfPlay", false);
        String mode = getArg(args, "mode", "exhaustive");
        int totalMatchups = getArg(args, "matchups", 1000);
        String playerDirectory = getArg(args, "dir", "");
        String logFile = getArg(args, "logFile", "");

        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        if (!playerDirectory.equals("")) {
            File dir = new File(playerDirectory);
            if (dir.exists() && dir.isDirectory()) {
                for (String fileName : dir.list()) {
                    System.out.println(fileName);
                    AbstractPlayer player = PlayerFactory.createPlayer(dir.getAbsolutePath() + File.separator + fileName);
                    agents.add(player);
                    player.setName(fileName.substring(0, fileName.indexOf(".")));
                }
            } else {
                throw new AssertionError("Specified dir argument is not a directory : " + playerDirectory);
            }
        } else {
            /* 2. Set up players */
            agents.add(new MCTSPlayer());
            agents.add(new BasicMCTSPlayer());
            agents.add(new RMHCPlayer());
            agents.add(new OSLAPlayer());
        }

        // Run!
        RoundRobinTournament tournament = mode.equals("exhaustive") ?
                new RoundRobinTournament(agents, gameToPlay, nPlayersPerGame, nGamesPerMatchUp, selfPlay) :
                new RandomRRTournament(agents, gameToPlay, nPlayersPerGame, nGamesPerMatchUp, selfPlay, totalMatchups,
                        System.currentTimeMillis());
        tournament.dataLogger = logFile.equals("") ? null : new FileStatsLogger(logFile, "\t", true);
        tournament.runTournament();
    }

    /**
     * Create a round robin tournament, which plays all agents against all others.
     *
     * @param agents          - players for the tournament.
     * @param gameToPlay      - game to play in this tournament.
     * @param playersPerGame  - number of players per game.
     * @param gamesPerMatchUp - number of games for each combination of players.
     * @param selfPlay        - true if agents are allowed to play copies of themselves.
     */
    public RoundRobinTournament(LinkedList<AbstractPlayer> agents, GameType gameToPlay, int playersPerGame,
                                int gamesPerMatchUp, boolean selfPlay) {
        super(agents, gameToPlay, playersPerGame);
        if (!selfPlay && playersPerGame >= this.agents.size()) {
            throw new IllegalArgumentException("Not enough agents to fill a match without self-play." +
                    "Either add more agents, reduce the number of players per game, or allow self-play.");
        }

        this.agentIDs = new LinkedList<>();
        for (int i = 0; i < this.agents.size(); i++)
            this.agentIDs.add(i);

        this.gamesPerMatchUp = gamesPerMatchUp;
        this.selfPlay = selfPlay;
        this.pointsPerPlayer = new int[agents.size()];
    }

    /**
     * Runs the round robin tournament.
     */
    @Override
    public void runTournament() {
        for (int g = 0; g < games.size(); g++) {
            System.out.println("Playing " + games.get(g).getGameType().name());

            LinkedList<Integer> matchUp = new LinkedList<>();
            createAndRunMatchUp(matchUp, g);

            for (int i = 0; i < this.agents.size(); i++) {
                System.out.printf("%s got %d points %n", agents.get(i), pointsPerPlayer[i]);
                System.out.printf("%s won %.1f%% of the games %n", agents.get(i), 100.0 * pointsPerPlayer[i] / (gamesPerMatchUp * matchUpsRun));
            }
        }
        if (dataLogger != null)
            dataLogger.processDataAndFinish();
    }

    /**
     * Recursively creates one combination of players and evaluates it.
     *
     * @param matchUp - current combination of players, updated recursively.
     * @param gameIdx - index of game to play with this match-up.
     */
    public void createAndRunMatchUp(LinkedList<Integer> matchUp, int gameIdx) {
        if (matchUp.size() == playersPerGame.get(gameIdx)) {
            evaluateMatchUp(matchUp, gameIdx);
        } else {
            for (Integer agentID : this.agentIDs) {
                if (selfPlay || !matchUp.contains(agentID)) {
                    matchUp.add(agentID);
                    createAndRunMatchUp(matchUp, gameIdx);
                    matchUp.remove(agentID);
                }
            }
        }
    }

    /**
     * Evaluates one combination of players.
     *
     * @param agentIDs - IDs of agents participating in this run.
     * @param gameIdx  - index of game to play in this evaluation.
     */
    protected void evaluateMatchUp(List<Integer> agentIDs, int gameIdx) {
        System.out.println("Evaluate " + agentIDs.toString());
        LinkedList<AbstractPlayer> matchUpPlayers = new LinkedList<>();
        for (int agentID : agentIDs)
            matchUpPlayers.add(this.agents.get(agentID));

        // Run the game N = gamesPerMatchUp times with these players
        for (int i = 0; i < this.gamesPerMatchUp; i++) {
            gameCounter++;
            games.get(gameIdx).reset(matchUpPlayers);
            games.get(gameIdx).run(null);  // Always running tournaments without visuals
            GameResult[] results = games.get(gameIdx).getGameState().getPlayerResults();
            for (int j = 0; j < matchUpPlayers.size(); j++) {
                pointsPerPlayer[agentIDs.get(j)] += results[j] == GameResult.WIN ? 1 : 0;
            }
            if (dataLogger != null) {
                Game g = games.get(gameIdx);
                for (int p = 0; p < g.getPlayers().size(); p++) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("GameId", gameCounter);
                    data.put("Game", g.getGameType().name());
                    data.put("PlayerNumber", p);
                    data.put("PlayerType", g.getPlayers().get(p).toString());
                    data.put("Score", g.getGameState().getHeuristicScore(p));
                    data.put("Result", g.getGameState().getPlayerResults()[p].toString());
                    dataLogger.record(data);
                }
            }
        }
        matchUpsRun++;
    }
}
