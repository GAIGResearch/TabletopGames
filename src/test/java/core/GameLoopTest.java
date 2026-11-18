package core;

import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.DISQUALIFY;
import static org.junit.Assert.*;

/**
 * Checks the main Game loop: overrides by listeners, timeouts, and the per-turn statistics.
 * TicTacToe is used as a host game with one decision per turn, CantStop as one with several decisions per turn.
 */
public class GameLoopTest {

    private Game ticTacToe(List<AbstractPlayer> players) {
        Game game = GameType.TicTacToe.createGameInstance(2, 42);
        game.reset(players);
        return game;
    }

    private Game timedOutTicTacToe(boolean disqualify) {
        Game game = GameType.TicTacToe.createGameInstance(2, 42);
        // no thinking time at all, so every decision is a timeout
        game.getGameState().getGameParameters().setThinkingTimeMins(0);
        game.getCoreParameters().disqualifyPlayerOnTimeout = disqualify;
        game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
        return game;
    }

    @Test
    public void overrideActionFromListenerIsAppliedInsteadOfPlayersChoice() {
        RecordingPlayer player0 = new RecordingPlayer();
        Game game = ticTacToe(List.of(player0, new RandomPlayer()));
        OverridingListener listener = new OverridingListener();
        game.addListener(listener);

        AbstractAction applied = game.oneAction();

        assertNotNull(listener.chosen);
        assertNotEquals(listener.chosen, listener.overrideWith);
        assertEquals(listener.overrideWith, applied);
        List<Pair<Integer, AbstractAction>> history = game.getGameState().getHistory();
        assertEquals(1, history.size());
        assertEquals(listener.overrideWith, history.getFirst().b);
        assertEquals(List.of(listener.overrideWith), listener.taken);
        assertEquals("player is told about the override", List.of(new Pair<>(listener.chosen, listener.overrideWith)), player0.overrides);
    }

    @Test
    public void overrideIsOnlyUsedOnce() {
        Game game = ticTacToe(List.of(new RandomPlayer(), new RandomPlayer()));
        OverridingListener listener = new OverridingListener();
        game.addListener(listener);

        game.oneAction();
        game.oneAction();

        assertEquals(2, game.getGameState().getHistory().size());
        assertEquals("second action is the player's own choice", listener.secondChosen, game.getGameState().getHistory().get(1).b);
    }

    @Test
    public void timeoutWithRandomActionAppliesExactlyOneAction() {
        Game game = timedOutTicTacToe(false);

        game.oneAction();

        assertEquals(1, game.getGameState().getHistory().size());
        assertEquals(1, game.getGameState().getGameTick());
        assertEquals(1, game.getGameState().getCurrentPlayer());
    }

    @Test
    public void timeoutWithDisqualificationAppliesNoAction() {
        Game game = timedOutTicTacToe(true);

        game.oneAction();

        AbstractGameState state = game.getGameState();
        assertEquals(DISQUALIFY, state.getPlayerResults()[0]);
        assertTrue("no action should be applied for a disqualified player", state.getHistory().isEmpty());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void randomActionOnTimeoutComesFromTheGameRandomNumberGenerator() {
        Game game = ticTacToe(List.of(new RandomPlayer(), new RandomPlayer()));
        AbstractGameState state = game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        state.rnd = new Random(999);
        int expected = new Random(999).nextInt(actions.size());
        // the parameters' seed would pick a different action
        assertNotEquals(expected, new Random(state.getGameParameters().getRandomSeed()).nextInt(actions.size()));

        assertEquals(actions.get(expected), fm.disqualifyOrRandomAction(false, state));
    }

    @Test
    public void actionsPerTurnIsOneWhenEachTurnIsOneAction() {
        Game game = ticTacToe(List.of(new RandomPlayer(), new RandomPlayer()));
        game.run();

        int nActions = game.getGameState().getHistory().size();
        assertEquals(nActions, game.getNActionsPerTurn());
        assertEquals(1.0, game.getMeanActionsPerTurn(), 1e-9);
    }

    @Test
    public void actionsPerTurnCountsConsecutiveActionsByTheSamePlayer() {
        Game game = GameType.CantStop.createGameInstance(2, 42);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
        game.run();

        List<Pair<Integer, AbstractAction>> history = game.getGameState().getHistory();
        int turns = 0;
        int lastPlayer = -1;
        for (Pair<Integer, AbstractAction> entry : history) {
            if (entry.a != lastPlayer) turns++;
            lastPlayer = entry.a;
        }
        assertTrue("CantStop should have several actions per turn", turns < history.size());
        assertEquals(history.size(), game.getNActionsPerTurn());
        assertEquals(history.size() / (double) turns, game.getMeanActionsPerTurn(), 1e-9);
    }

    @Test
    public void flipPausedTogglesPause() {
        Game game = ticTacToe(List.of(new RandomPlayer(), new RandomPlayer()));
        game.setPaused(false);
        game.flipPaused();
        assertTrue(game.isPaused());
        game.flipPaused();
        assertFalse(game.isPaused());
    }

    static class RecordingPlayer extends RandomPlayer {
        final List<Pair<AbstractAction, AbstractAction>> overrides = new ArrayList<>();

        @Override
        public void overrideAction(AbstractAction originalAction, AbstractAction override) {
            overrides.add(new Pair<>(originalAction, override));
        }
    }

    /**
     * Overrides the first decision of the game with a different action, and records what is chosen and taken.
     */
    static class OverridingListener implements IGameListener {
        Game game;
        AbstractAction chosen, secondChosen, overrideWith;
        final List<AbstractAction> taken = new ArrayList<>();

        @Override
        public void onEvent(Event event) {
            if (event.type == Event.GameEvent.ACTION_CHOSEN) {
                if (chosen == null) {
                    chosen = event.action;
                    overrideWith = event.actions.stream().filter(a -> !a.equals(chosen)).findFirst().orElseThrow();
                    game.setOverrideAction(overrideWith);
                } else if (secondChosen == null) {
                    secondChosen = event.action;
                }
            } else if (event.type == Event.GameEvent.ACTION_TAKEN) {
                taken.add(event.action);
            }
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
}
