package games.connect4;

import core.AbstractGameState;
import core.Game;
import evaluation.metrics.Event;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Guards the contract between what {@link Connect4Metrics.ColumnPieceCount} records and the columns it declares:
 * a record key without a declared column is invisible in the resulting data, and a declared column without a
 * record leaves gaps.
 */
public class Connect4MetricsTest {

    private static AbstractGameState stateWith(int width, int height) {
        Connect4GameParameters params = new Connect4GameParameters();
        params.width = width;
        params.height = height;
        params.winCount = 4;
        Game game = games.GameType.Connect4.createGameInstance(2, 42L, params);
        return game.getGameState();
    }

    @Test
    public void recordedKeysMatchTheDeclaredColumns() {
        AbstractGameState state = stateWith(7, 6);
        Connect4Metrics.ColumnPieceCount metric = new Connect4Metrics.ColumnPieceCount();
        Map<String, Object> records = new HashMap<>();

        metric._run(null, Event.createEvent(Event.GameEvent.GAME_OVER, state), records);

        assertEquals(metric.getColumns(2, null).keySet(), records.keySet());
    }

    @Test
    public void aBoardNarrowerThanTheColumnCountStillFillsEveryColumn() {
        AbstractGameState state = stateWith(5, 4);
        Connect4Metrics.ColumnPieceCount metric = new Connect4Metrics.ColumnPieceCount();
        Map<String, Object> records = new HashMap<>();

        metric._run(null, Event.createEvent(Event.GameEvent.GAME_OVER, state), records);

        assertEquals(Connect4Metrics.ColumnPieceCount.MAX_COLUMNS, records.size());
        for (Object value : records.values())
            assertEquals("empty columns must record 0", 0, value);
    }

    @Test
    public void aBoardWiderThanTheColumnCountFailsLoudly() {
        AbstractGameState state = stateWith(Connect4Metrics.ColumnPieceCount.MAX_COLUMNS + 1, 4);
        Connect4Metrics.ColumnPieceCount metric = new Connect4Metrics.ColumnPieceCount();

        try {
            metric._run(null, Event.createEvent(Event.GameEvent.GAME_OVER, state), new HashMap<>());
            fail("a board wider than the declared columns must not be recorded silently");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("MAX_COLUMNS"));
        }
    }
}
