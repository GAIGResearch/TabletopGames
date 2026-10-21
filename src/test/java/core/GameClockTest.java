package core;

import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static utilities.ElapsedCpuTimerTest.spin;

public class GameClockTest {

    /** Always picks the last action, spending the given time on each decision. */
    static class SlowPlayer extends AbstractPlayer {
        final long thinkMillis;
        final List<AbstractAction> chosen = new ArrayList<>();

        SlowPlayer(long thinkMillis) {
            super(null, "SlowPlayer");
            this.thinkMillis = thinkMillis;
        }

        @Override
        public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
            spin(thinkMillis);
            AbstractAction action = possibleActions.get(possibleActions.size() - 1);
            chosen.add(action);
            return action;
        }

        @Override
        public SlowPlayer copy() {
            return new SlowPlayer(thinkMillis);
        }
    }

    static class ActionTakenRecorder implements IGameListener {
        final List<Event> taken = new ArrayList<>();
        Game game;

        @Override
        public void onEvent(Event event) {
            if (event.type == Event.GameEvent.ACTION_TAKEN)
                taken.add(event);
        }

        @Override
        public void report() {
        }

        @Override
        public void setGame(Game game) {
            this.game = game;
        }

        @Override
        public Game getGame() {
            return game;
        }
    }

    private Game ticTacToe(long thinkingTimeMillis, AbstractPlayer... players) {
        return ticTacToe(42, thinkingTimeMillis, players);
    }

    private Game ticTacToe(long seed, long thinkingTimeMillis, AbstractPlayer... players) {
        AbstractParameters params = new TicTacToeGameParameters();
        params.setRandomSeed(seed);
        params.setThinkingTimeMillis(thinkingTimeMillis);
        return new Game(GameType.TicTacToe, List.of(players), new TicTacToeForwardModel(),
                new TicTacToeGameState(params, players.length));
    }

    @Test
    public void parametersCopyKeepsClockAndRoundSettings() {
        AbstractParameters params = new TicTacToeGameParameters();
        params.setThinkingTimeMillis(1_234);
        params.incrementActionS = 2;
        params.incrementTurnS = 3;
        params.incrementRoundS = 4;
        params.incrementMilestoneS = 5;
        params.setMaxRounds(7);
        params.setTimeoutRounds(8);

        AbstractParameters copy = params.copy();
        assertEquals(1_234, copy.getThinkingTimeMillis());
        assertEquals(2, copy.getIncrementActionS());
        assertEquals(3, copy.getIncrementTurnS());
        assertEquals(4, copy.getIncrementRoundS());
        assertEquals(5, copy.getIncrementMilestoneS());
        assertEquals(7, copy.getMaxRounds());
        assertEquals(8, copy.getTimeoutRounds());
        assertEquals(params, copy);
    }

    @Test
    public void playerClocksStartAtTheConfiguredThinkingTime() {
        Game game = ticTacToe(2_500, new RandomPlayer(new Random(1)), new RandomPlayer(new Random(2)));
        for (int p = 0; p < 2; p++)
            assertEquals(2_500, game.getGameState().getPlayerTimer()[p].remainingTimeMillis());
    }

    @Test
    public void clocksAreDebitedAndNotExceededByFastPlayers() {
        Game game = ticTacToe(10_000, new SlowPlayer(20), new SlowPlayer(20));
        game.run();
        for (int p = 0; p < 2; p++) {
            long remaining = game.getGameState().getPlayerTimer()[p].remainingTimeMillis();
            assertTrue("player " + p + " remaining " + remaining, remaining > 0 && remaining <= 10_000 - 15);
        }
    }

    @Test
    public void timeoutIsDetectedOnTheMoveThatOverruns() {
        // Player 0 overruns its 100 ms clock on its very first move. The random replacement can coincide with
        // the player's own choice, so we look across several seeds for one where it visibly differs.
        boolean replaced = false;
        for (long seed = 1; seed <= 5 && !replaced; seed++) {
            SlowPlayer slow = new SlowPlayer(150);
            Game game = ticTacToe(seed, 100, slow, new RandomPlayer(new Random(3)));
            ActionTakenRecorder recorder = new ActionTakenRecorder();
            game.addListener(recorder);
            game.run();

            Event first = recorder.taken.get(0);
            assertEquals(0, first.playerID);
            assertTrue(game.getGameState().getPlayerTimer()[0].exceededMaxTime());
            replaced = !slow.chosen.get(0).equals(first.action);
        }
        assertTrue("first move should have been replaced", replaced);
    }

    @Test
    public void randomReplacementOnTimeoutIsAppliedOnce() {
        // Both players run out of time early, so most of the game is replacement actions
        Game game = ticTacToe(50, new SlowPlayer(30), new SlowPlayer(30));
        ActionTakenRecorder recorder = new ActionTakenRecorder();
        game.addListener(recorder);
        game.run();

        assertEquals(recorder.taken.size(), game.getGameState().getHistory().size());
        for (Event e : recorder.taken)
            assertEquals(e.action, game.getGameState().getHistory().get(recorder.taken.indexOf(e)).b);
    }
}
