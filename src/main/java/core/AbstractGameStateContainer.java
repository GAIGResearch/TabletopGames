package core;

import games.GameType;

import java.util.List;

public abstract class AbstractGameStateContainer {
    // Game being played
    public final GameType gameType;

    // Game tick, number of iterations of game loop
    public final int tick;

    public final int roundCounter, turnCounter, turnOwner, firstPlayer;
    public final int nPlayers;
    public final int nTeams;

    // Status of the game, and status for each player (in cooperative games, the game status is also each player's status)
    public final CoreConstants.GameResult gameStatus;
    public final CoreConstants.GameResult[] playerResults;

    // A record of all actions taken to reach this game state, in chronological order
    public final List<String> historyText;

    protected AbstractGameStateContainer(AbstractGameState gs) {
        this.gameType = gs.getGameType();

        this.tick = gs.getGameTick();
        this.roundCounter = gs.getRoundCounter();
        this.turnCounter = gs.getTurnCounter();
        this.turnOwner = gs.getTurnOwner();

        this.firstPlayer = gs.getFirstPlayer();
        this.nPlayers = gs.getNPlayers();
        this.nTeams = gs.getNTeams();

        this.gameStatus = gs.getGameStatus();
        this.playerResults = gs.getPlayerResults();

        this.historyText = gs.getHistoryAsText();
    }
}
