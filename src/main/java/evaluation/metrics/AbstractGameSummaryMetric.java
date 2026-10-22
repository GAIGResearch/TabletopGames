package evaluation.metrics;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;

import java.util.*;

/**
 * Records one row per game at GAME_OVER: the game length, and for each player their type, final score, ordinal
 * position and result. Subclasses add game-specific columns, and may accumulate counts from the ACTION_CHOSEN events
 * of the game in {@link #onAction}.
 * <p>
 * Ordinal positions are by game score alone, with tied players sharing the better position.
 */
public abstract class AbstractGameSummaryMetric extends AbstractMetric {

    private int gameID = -1;
    private int decisions;
    private int actions;

    @Override
    public Set<IGameEvent> getDefaultEventTypes() {
        return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.GAME_OVER));
    }

    /**
     * ACTION_CHOSEN events only feed the counts recorded at GAME_OVER.
     */
    @Override
    public boolean recordsDataFor(IGameEvent eventType) {
        return eventType == Event.GameEvent.GAME_OVER;
    }

    @Override
    protected final boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
        if (e.state.getGameID() != gameID) {
            gameID = e.state.getGameID();
            decisions = 0;
            actions = 0;
            resetGame(e.state);
        }
        if (e.type == Event.GameEvent.ACTION_CHOSEN) {
            actions++;
            if (e.actions.size() > 1)
                decisions++;
            onAction(e);
            return false;
        }
        AbstractGameState state = e.state;
        records.put("Decisions", decisions);
        records.put("Actions", actions);
        for (int i = 0; i < state.getNPlayers(); i++) {
            records.put("Player-" + i + " Type", listener.getGame().getPlayers().get(i).toString());
            records.put("Player-" + i + " Score", state.getGameScore(i));
            records.put("Player-" + i + " Position", state.getOrdinalPosition(i, state::getGameScore, null));
            records.put("Player-" + i + " Result", result(state.getPlayerResults()[i]));
        }
        recordGameOver(e, records);
        return true;
    }

    private static String result(CoreConstants.GameResult result) {
        return switch (result) {
            case WIN_GAME -> "Win";
            case DRAW_GAME -> "Draw";
            case LOSE_GAME -> "Loss";
            default -> result.name();
        };
    }

    /**
     * Clears the counts accumulated for the previous game.
     */
    protected abstract void resetGame(AbstractGameState state);

    /**
     * Called for every ACTION_CHOSEN event, with the state before the action is applied.
     */
    protected abstract void onAction(Event e);

    /**
     * Adds the game-specific columns at GAME_OVER.
     */
    protected abstract void recordGameOver(Event e, Map<String, Object> records);

    /**
     * The game-specific columns.
     */
    protected abstract Map<String, Class<?>> getGameColumns(int nPlayersPerGame);

    @Override
    public final Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
        Map<String, Class<?>> columns = new HashMap<>();
        columns.put("Decisions", Integer.class);
        columns.put("Actions", Integer.class);
        for (int i = 0; i < nPlayersPerGame; i++) {
            columns.put("Player-" + i + " Type", String.class);
            columns.put("Player-" + i + " Score", Double.class);
            columns.put("Player-" + i + " Position", Integer.class);
            columns.put("Player-" + i + " Result", String.class);
        }
        columns.putAll(getGameColumns(nPlayersPerGame));
        return columns;
    }
}
