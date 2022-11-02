package evaluation;

import core.*;
import games.GameType;
import utilities.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractTournament {
    // List of players taking part in the tournament
    protected List<AbstractPlayer> agents;
    // Games to play
    protected List<Game> games;
    // Number of players in the games, index matches the games list
    protected List<Integer> playersPerGame;

    /**
     * Constructor, initialises the tournament given a list of players, a game to play and the number of players
     * in the game.
     * @param agents - players taking part in this tournament.
     * @param gameToPlay - game to play in this tournament.
     * @param nPlayerPerGame - number of players per game.
     */
    public AbstractTournament(List<AbstractPlayer> agents, GameType gameToPlay, int nPlayerPerGame, AbstractParameters gameParams){
        this.agents = agents;
        this.games = new ArrayList<>();
        this.playersPerGame = new ArrayList<>();

        Game g = gameParams == null ?
                gameToPlay.createGameInstance(nPlayerPerGame) :
                gameToPlay.createGameInstance(nPlayerPerGame, gameParams);

        if (g == null) throw new IllegalArgumentException("Chosen game not supported");
        else {
            this.games.add(g);
            this.playersPerGame.add(nPlayerPerGame);
        }
    }

    /**
     * Constructor, initialises the tournament given a list of players and a list of games to play.
     * @param agents - list of players taking part in the tournament.
     * @param gamesToPlay - list of games to play in the tournament, in pairs of (GameType, nPlayerPerGame).
     */
    public AbstractTournament(LinkedList<AbstractPlayer> agents, List<Pair<GameType, Integer>> gamesToPlay) {
        this.agents = agents;
        this.games = new ArrayList<>();
        this.playersPerGame = new ArrayList<>();

        for (Pair<GameType, Integer> gameToPlay: gamesToPlay) {
            Game g = gameToPlay.a.createGameInstance(gameToPlay.b);
            if (g == null) throw new IllegalArgumentException("Chosen game not supported");
            else {
                this.games.add(g);
                this.playersPerGame.add(gameToPlay.b);
            }
        }
    }

    /**
     * Runs the tournament in the given game, with the given players.
     */
    public abstract void runTournament();
}
