package evaluation;

import core.AbstractPlayer;
import games.GameType;
import players.mcts.BasicMCTSPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCPlayer;
import utilities.Utils;

import java.util.LinkedList;

import static games.GameType.*;


public class RoundRobinTournament extends AbstractTournament {
    int[] pointsPerPlayer;
    LinkedList<Integer> agentIDs;
    private final int gamesPerMatchUp;
    private int matchUpsRun;
    private final boolean selfPlay;

    /**
     * Main function, creates an runs the tournament with the given settings and players.
     */
    @SuppressWarnings({"UnnecessaryLocalVariable", "ConstantConditions"})
    public static void main(String[] args){
        /* 1. Settings for the tournament */
        GameType gameToPlay = Uno;
        int nPlayersTotal = 4;
        int nPlayersPerGame = 3;
        int nGamesPerMatchUp = 10;
        boolean selfPlay = false;

        /* 2. Set up players */
        LinkedList<AbstractPlayer> agents = new LinkedList<>();
        agents.add(new RandomPlayer());
        agents.add(new BasicMCTSPlayer());
        agents.add(new RMHCPlayer());
        agents.add(new OSLAPlayer());

        // Run!
        AbstractTournament tournament = new RoundRobinTournament(agents, gameToPlay, nPlayersPerGame, nGamesPerMatchUp, selfPlay);
        tournament.runTournament();
    }

    /**
     * Create a round robin tournament, which plays all agents against all others.
     * @param agents - players for the tournament.
     * @param gameToPlay - game to play in this tournament.
     * @param playersPerGame - number of players per game.
     * @param gamesPerMatchUp - number of games for each combination of players.
     * @param selfPlay - true if agents are allowed to play copies of themselves.
     */
    public RoundRobinTournament(LinkedList<AbstractPlayer> agents, GameType gameToPlay, int playersPerGame,
                                int gamesPerMatchUp, boolean selfPlay){
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
                System.out.println(String.format("%s got %d points ", agents.get(i), pointsPerPlayer[i]));
                System.out.println(String.format("%s won %.1f%% of the games ", agents.get(i), 100.0 * pointsPerPlayer[i] / (gamesPerMatchUp * matchUpsRun)));
            }
        }
    }

    /**
     * Recursively creates one combination of players and evaluates it.
     * @param matchUp - current combination of players, updated recursively.
     * @param gameIdx - index of game to play with this match-up.
     */
    public void createAndRunMatchUp(LinkedList<Integer> matchUp, int gameIdx){
        if (matchUp.size() == playersPerGame.get(gameIdx)){
            evaluateMatchUp(matchUp, gameIdx);
        }
        else {
            for (Integer agentID : this.agentIDs){
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
     * @param agentIDs - IDs of agents participating in this run.
     * @param gameIdx - index of game to play in this evaluation.
     */
    private void evaluateMatchUp(LinkedList<Integer> agentIDs, int gameIdx){
        System.out.println("Evaluate " + agentIDs.toString());
        LinkedList<AbstractPlayer> matchUpPlayers = new LinkedList<>();
        for (int agentID : agentIDs)
            matchUpPlayers.add(this.agents.get(agentID));

        // Run the game N = gamesPerMatchUp times with these players
        for (int i = 0; i < this.gamesPerMatchUp; i++) {
            games.get(gameIdx).reset(matchUpPlayers);
            games.get(gameIdx).run(null);  // Always running tournaments without visuals
            Utils.GameResult[] results = games.get(gameIdx).getGameState().getPlayerResults();
            for (int j = 0; j < matchUpPlayers.size(); j++) {
                pointsPerPlayer[agentIDs.get(j)] += results[j] == Utils.GameResult.WIN ? 1 : 0;
            }
        }
        matchUpsRun++;
    }
}
