package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import games.GameType;
import org.junit.Test;
import players.mcts.MCTSPlayer;
import players.simple.RandomPlayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GameAdviserTest {

    @Test
    public void testLogging() throws IOException {
        String fileName = "test_adviser_log.txt";
        File file = new File(fileName);
        if (file.exists()) file.delete();

        AbstractPlayer adviserPlayer = new RandomPlayer();
        IAdviceFilter filter = new IAdviceFilter() {
            @Override
            public boolean payAttention(core.AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee) {
                return true;
            }

            @Override
            public boolean provideAdvice(core.AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee, AbstractAction advice, GameAdviser adviser) {
                return true;
            }
        };

        GameAdviser adviser = new GameAdviser(adviserPlayer, filter, fileName);
        
        // Setup a game
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        Game game = GameType.TicTacToe.createGameInstance(2);
        game.reset(players);
        adviser.setGame(game);
        
        // Manually trigger an event
        AbstractGameState state = game.getGameState();
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
        AbstractAction action = actions.get(0);
        Event event = Event.createEvent(Event.GameEvent.ACTION_CHOSEN, state, action, actions, state.getCurrentPlayer());
        
        adviser.onEvent(event);
        adviser.report(); // Should close the file

        assertTrue("File should exist", file.exists());
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String header = reader.readLine();
            assertNotNull("Header should not be null", header);
            assertEquals("PlayerID\tAgentName\tAgentAction\tAgentValue\tAdviserAction\tAdviserValue\tGameID\tTurn\tRound\tTick", header);
            
            String logLine = reader.readLine();
            assertNotNull("Log line should not be null", logLine);
            String[] parts = logLine.split("\t");
            assertEquals("Should have 10 parts", 10, parts.length);
        } finally {
            file.delete();
        }
    }

    @Test
    public void testSetOutputDirectory() {
        AbstractPlayer adviserPlayer = new RandomPlayer();
        IAdviceFilter filter = new UnderdogAdviser();
        GameAdviser adviser = new GameAdviser(adviserPlayer, filter, "test.txt");
        
        adviser.setOutputDirectory("testDir1", "testDir2");
        
        String expectedPath = "testDir1" + File.separator + "testDir2" + File.separator + "test.txt";
        assertEquals(new File(expectedPath).getAbsolutePath(), new File(adviser.fileName).getAbsolutePath());
        
        File file = new File(adviser.fileName);
        assertTrue("File should exist in the new directory", file.exists());
        
        adviser.report();
        file.delete();
        new File("testDir1" + File.separator + "testDir2").delete();
        new File("testDir1").delete();
        new File("test.txt").delete(); // The one created in constructor
    }

    @Test
    public void testMCTSLogging() throws IOException {
        String fileName = "mcts_adviser_log.txt";
        File file = new File(fileName);
        if (file.exists()) file.delete();

        MCTSPlayer adviserPlayer = new MCTSPlayer();
        
        // Setup a game
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(new RandomPlayer());
        players.add(new RandomPlayer());
        Game game = GameType.TicTacToe.createGameInstance(2);
        game.reset(players);
        adviserPlayer.setForwardModel(game.getForwardModel());

        IAdviceFilter filter = new IAdviceFilter() {
            @Override
            public boolean payAttention(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee) {
                return true;
            }

            @Override
            public boolean provideAdvice(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee, AbstractAction advice, GameAdviser adviser) {
                return true;
            }
        };

        GameAdviser adviser = new GameAdviser(adviserPlayer, filter, fileName);
        adviser.setGame(game);

        // Run one action to populate MCTS tree
        AbstractGameState state = game.getGameState();
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
        AbstractAction action = adviserPlayer.getAction(state, actions);

        // Manually trigger an event
        Event event = Event.createEvent(Event.GameEvent.ACTION_CHOSEN, state, action, actions, state.getCurrentPlayer());

        adviser.onEvent(event);
        adviser.report();

        assertTrue("File should exist", file.exists());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header
            String logLine = reader.readLine();
            assertNotNull("Log line should not be null", logLine);
            String[] parts = logLine.split("\t");
            assertEquals("Should have 10 parts", 10, parts.length);
            // Check that values are not "0.0" if possible, but TicTacToe might have 0.0 values.
            // At least it didn't crash.
        } finally {
            file.delete();
        }
    }
}
