package evaluation.listeners;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.AbstractGameState;
import core.Game;
import evaluation.metrics.Event;
import games.GameType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * The listener that replaced {@code GameMetrics.SaveStateOnEvent}: it should take its directory from the
 * framework rather than from configuration, honour the event selection, and refuse a game it cannot snapshot.
 */
public class SaveStateListenerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private AbstractGameState cantStop;

    @Before
    public void setup() {
        GameType gt = GameType.CantStop;
        Game game = gt.createGameInstance(2, 42L, gt.createParameters(42L));
        game.reset(List.of(new players.simple.RandomPlayer(new Random(1)), new players.simple.RandomPlayer(new Random(2))));
        cantStop = game.getGameState();
    }

    @Test
    public void writesAContainerSnapshotForAConfiguredEvent() throws Exception {
        SaveStateListener listener = new SaveStateListener();
        assertTrue(listener.setOutputDirectory(tmp.getRoot().getAbsolutePath()));

        listener.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, cantStop));

        File[] files = new File(tmp.getRoot(), "rawGameStates").listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
        assertTrue("unexpected name: " + files[0].getName(),
                files[0].getName().matches("CantStop-\\d+-\\d+-\\d+\\.json"));

        try (FileReader reader = new FileReader(files[0])) {
            JsonObject snapshot = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("CantStop", snapshot.get("gameType").getAsString());
            assertEquals(2, snapshot.get("nPlayers").getAsInt());
            assertTrue(snapshot.has("completedColumns"));
        }
    }

    @Test
    public void ignoresEventsThatWereNotAskedFor() {
        SaveStateListener listener = new SaveStateListener();
        listener.setOutputDirectory(tmp.getRoot().getAbsolutePath());

        listener.onEvent(Event.createEvent(Event.GameEvent.ACTION_CHOSEN, cantStop));

        File[] files = new File(tmp.getRoot(), "rawGameStates").listFiles();
        assertNotNull(files);
        assertEquals(0, files.length);
    }

    @Test
    public void eventSelectionIsHonouredAndTheLastSnapshotWins() {
        SaveStateListener listener = new SaveStateListener(new String[]{"ROUND_OVER"});
        listener.setOutputDirectory(tmp.getRoot().getAbsolutePath());

        listener.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, cantStop));   // not selected
        assertEquals("TURN_OVER was not selected", 0, new File(tmp.getRoot(), "rawGameStates").listFiles().length);

        listener.onEvent(Event.createEvent(Event.GameEvent.ROUND_OVER, cantStop));  // selected
        assertEquals(1, new File(tmp.getRoot(), "rawGameStates").listFiles().length);

        // GAME_OVER is always written, and lands on the same name because the position has not moved,
        // so it replaces the round-over snapshot rather than adding a second file.
        listener.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, cantStop));
        assertEquals(1, new File(tmp.getRoot(), "rawGameStates").listFiles().length);
    }

    @Test
    public void refusesAGameWithNoContainer() {
        GameType gt = GameType.TicTacToe;
        Game game = gt.createGameInstance(2, 7L);
        game.reset(List.of(new players.simple.RandomPlayer(), new players.simple.RandomPlayer()));
        SaveStateListener listener = new SaveStateListener();
        listener.setOutputDirectory(tmp.getRoot().getAbsolutePath());

        try {
            listener.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, game.getGameState()));
            fail("expected IllegalStateException for a game without a container");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("TicTacToe"));
            assertTrue(expected.getMessage().contains("GameStateContainerFactory"));
        }
    }

    @Test
    public void refusesToReuseAnExistingDirectory() throws Exception {
        File existing = tmp.newFolder("rawGameStates");
        SaveStateListener listener = new SaveStateListener();
        try {
            listener.setOutputDirectory(tmp.getRoot().getAbsolutePath());
            fail("expected RuntimeException when rawGameStates already exists");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("ALREADY EXIST"));
            assertTrue("the existing folder must be untouched", existing.isDirectory());
        }
    }

    @Test
    public void explainsItselfWhenNoDirectoryWasGiven() {
        SaveStateListener listener = new SaveStateListener();
        try {
            listener.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, cantStop));
            fail("expected IllegalStateException without an output directory");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("setOutputDirectory"));
        }
    }

    @Test
    public void writesSnapshotsDuringARealGame() {
        SaveStateListener listener = new SaveStateListener();
        listener.setOutputDirectory(tmp.getRoot().getAbsolutePath());

        GameType gt = GameType.CantStop;
        Game.runOne(gt, null,
                List.of(new players.simple.RandomPlayer(new Random(3)), new players.simple.RandomPlayer(new Random(4))),
                99L, false, List.of(listener), null, 0);

        File[] files = new File(tmp.getRoot(), "rawGameStates").listFiles();
        assertNotNull(files);
        assertTrue("expected snapshots from a played game, found " + files.length, files.length >= 2);
        for (File f : files) {
            assertTrue("unexpected name: " + f.getName(),
                    f.getName().matches("CantStop-\\d+-\\d+-\\d+\\.json"));
        }
    }
}
