package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import games.GameType;
import utilities.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractTournament {


    public enum TournamentMode {
        SELF_PLAY,
        NO_SELF_PLAY,
        ONE_VS_ALL
    }
    // List of players taking part in the tournament
    protected List<? extends AbstractPlayer> agents;
    // Games to play
    protected Game games;
    // Number of players in the games, index matches the games list
    protected int playersPerGame;
    protected String resultsFile;
    // Filename to write the results of the tournament


    /**
     * Constructor, initialises the tournament given a list of players, a game to play and the number of players
     * in the game.
     *
     * @param agents         - players taking part in this tournament.
     * @param gameToPlay     - game to play in this tournament.
     * @param nPlayerPerGame - number of players per game.
     */
    public AbstractTournament(List<? extends AbstractPlayer> agents, GameType gameToPlay, int nPlayerPerGame, AbstractParameters gameParams) {
        this.agents = agents;

        this.games = gameParams == null ?
                gameToPlay.createGameInstance(nPlayerPerGame) :
                gameToPlay.createGameInstance(nPlayerPerGame, gameParams);

        this.playersPerGame = nPlayerPerGame;
    }


    /**
     * Runs the tournament in the given game, with the given players.
     * <p>
     * Returns the scores of players (in the same order the players were provided in the constructor).
     */
    public abstract void runTournament();
}
