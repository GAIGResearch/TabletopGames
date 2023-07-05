package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGameRunner;
import games.GameType;
import java.util.List;

public abstract class AbstractTournament implements IGameRunner {

    public enum TournamentMode {
        SELF_PLAY,
        NO_SELF_PLAY,
        ONE_VS_ALL
    }
    public final TournamentMode tournamentMode;
    // List of players taking part in the tournament
    protected List<? extends AbstractPlayer> agents;
    // Games to play
    protected Game game;
    // Number of players in the games, index matches the games list
    protected int nPlayers;
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
    public AbstractTournament(TournamentMode mode, List<? extends AbstractPlayer> agents, GameType gameToPlay,
                              int nPlayerPerGame, AbstractParameters gameParams) {
        this.tournamentMode = mode;
        this.agents = agents;

        this.game = gameParams == null ?
                gameToPlay.createGameInstance(nPlayerPerGame) :
                gameToPlay.createGameInstance(nPlayerPerGame, gameParams);

        this.nPlayers = nPlayerPerGame;
    }
}
