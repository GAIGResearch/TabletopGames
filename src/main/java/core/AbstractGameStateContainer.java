package core;

import core.AbstractGameState;
import core.AbstractParameters;
import core.CoreConstants;
import core.CoreParameters;
import core.actions.AbstractAction;
import core.components.Area;
import core.interfaces.IExtendedSequence;
import core.interfaces.IGamePhase;
import evaluation.listeners.IGameListener;
import games.GameType;
import utilities.ElapsedCpuChessTimer;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public abstract class AbstractGameStateContainer {
    // Parameters, forward model and turn order for the game
//    public final AbstractParameters gameParameters;
    // Game being played
    public final GameType gameType;

    // Game tick, number of iterations of game loop
    public final int tick;

    // Migrated from TurnOrder...may move later
    public final int roundCounter, turnCounter, turnOwner, firstPlayer;
    public final int nPlayers;
    public final int nTeams;

    // Timers for all players
//    protected ElapsedCpuChessTimer[] playerTimer;

    // Status of the game, and status for each player (in cooperative games, the game status is also each player's status)
    public final CoreConstants.GameResult gameStatus;
    public final CoreConstants.GameResult[] playerResults;
    // Current game phase
//    protected IGamePhase gamePhase;
    // Stack for extended actions
//    public final CoreParameters coreGameParameters;

    // A record of all actions taken to reach this game state
    // The history is stored as a list of pairs, where the first element is the player who took the action
    // this is in chronological order
//    public final List<Pair<Integer, AbstractAction>> history;
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

//        this.history = gs.getHistory();
        this.historyText = gs.getHistoryAsText();
    }
}
