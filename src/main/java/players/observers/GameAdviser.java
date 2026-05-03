package players.observers;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import org.jetbrains.annotations.NotNull;
import players.mcts.MCTSPlayer;
import utilities.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class GameAdviser implements IGameListener {

    protected Game game;
    protected AbstractPlayer player;
    private final IAdviceFilter filter;
    protected FileWriter writer;
    protected String fileName;

    /**
     * A Game Adviser is responsible for providing advice to one or more players during a game.
     * This may be one or all players in the game.
     *
     * The GameAdviser is composed of two elements:
     *      The AbstractPlayer that decides on what the Adviser thinks is best
     *      The IAdviceFilter that decides on which players in a game are advised, and other conditions
     *      about *when* the GameAdviser actually intervenes
     *
     * @param player the agent to be used by the adviser
     * @param filter the filter to (may be null)
     */
    public GameAdviser(@NotNull AbstractPlayer player, @NotNull IAdviceFilter filter) {
        this(player, filter, null);
    }

    public GameAdviser(@NotNull AbstractPlayer player, @NotNull IAdviceFilter filter, String fileName) {
        this.player = player;
        this.filter = filter;
        this.fileName = fileName == null ? this.getClass().getSimpleName() + ".txt" : fileName;
        setupWriter();
    }

    private void setupWriter() {
        try {
            writer = new FileWriter(this.fileName, true);
            File file = new File(this.fileName);
            if (file.length() == 0) {
                writer.write("PlayerID\tAgentName\tAgentAction\tAgentValue\tAdviserAction\tAdviserValue\tGameID\tTurn\tRound\tTick\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEvent(Event event) {
        int actingPlayerID = event.state.getCurrentPlayer();
        AbstractPlayer actingPlayer = game.getPlayers().get(actingPlayerID);
        if (event.type == Event.GameEvent.ACTION_CHOSEN && filter.payAttention(event.state, event.action, actingPlayer)) {
            AbstractAction action = player.getAction(event.state, event.actions);
            if (filter.provideAdvice(event.state, event.action, actingPlayer, action, this)) {
                game.setOverrideAction(action);
                logIntervention(event, action, actingPlayer);
            }
        }
    }

    @Override
    public void report() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing writer", e);
            }
        }
    }

    @Override
    public boolean setOutputDirectory(String... nestedDirectories) {
        if (writer != null) {
            report();
            writer = null;
        }
        String folder = Utils.createDirectory(nestedDirectories);
        this.fileName = folder + File.separator + this.fileName;
        setupWriter();
        return true;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }

    protected void logIntervention(Event event, AbstractAction action, AbstractPlayer actingPlayer) {
        if (writer == null) return;
        double agentValue = player instanceof MCTSPlayer mcts ? mcts.getValue(event.action) : 0.0;
        double adviserValue = player instanceof MCTSPlayer mcts ? mcts.getValue(action) : 0.0;

        try {
            writer.write(String.format("%s\t%s\t%s\t%.3g\t%s\t%.3g\t%d\t%d\t%d\t%d\n",
                    event.playerID, actingPlayer.toString(),
                    event.action.toString(), agentValue, action.toString(), adviserValue,
                    event.state.getGameID(), event.state.getTurnCounter(), event.state.getRoundCounter(), event.state.getGameTick()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
