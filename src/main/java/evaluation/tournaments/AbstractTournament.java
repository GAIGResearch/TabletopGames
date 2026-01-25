package evaluation.tournaments;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGameRunner;
import games.GameType;
import players.IAnyTimePlayer;

import java.util.List;

public abstract class AbstractTournament implements IGameRunner {

    /**
     * Enum to specify the mode of the tournament
     * RANDOM - Randomly select players to play against each other. This will only have two identical agents
     *          in one game if there are fewer agents than the number of players in the game.
     * EXHAUSTIVE - Play all agents against each other. With every permutation of agents to player positions.
     * EXHAUSTIVE_SELF_PLAY - Play all agents against each other, but allow for self-play.
     * ONE_VS_ALL - Play one agent against all others. The one (focus) agent will be the only agent of its type in each game.
     */
    public enum TournamentMode {
        RANDOM,
        EXHAUSTIVE,
        EXHAUSTIVE_SELF_PLAY,
        ONE_VS_ALL,
        FIXED
    }
    // List of players taking part in the tournament
    protected List<? extends AbstractPlayer> agents;
    // Games to play
    protected Game game;
    // Number of players in the games, index matches the games list
    protected int nPlayers;
    protected String resultsFile = "TournamentResults.txt";
    // Filename to write the results of the tournament


    /**
     * Constructor, initialises the tournament given a list of players, a game to play and the number of players
     * in the game.
     *
     * @param agents         - players taking part in this tournament.
     * @param gameToPlay     - game to play in this tournament.
     * @param nPlayerPerGame - number of players per game.
     */
    public AbstractTournament(List<? extends AbstractPlayer> agents, GameType gameToPlay,
                              int nPlayerPerGame, AbstractParameters gameParams) {
        this.agents = agents;

        this.game = gameParams == null ?
                gameToPlay.createGameInstance(nPlayerPerGame) :
                gameToPlay.createGameInstance(nPlayerPerGame, gameParams);

        this.nPlayers = nPlayerPerGame;
    }

    public void setResultsFile(String resultsFile) {
        this.resultsFile = resultsFile;
    }

    public Game getGame() {
        return game;
    }
}
