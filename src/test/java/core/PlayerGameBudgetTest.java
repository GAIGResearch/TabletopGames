package core;

import core.actions.AbstractAction;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import org.junit.Test;
import players.PlayerConstants;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import static utilities.ElapsedCpuTimerTest.spin;

public class PlayerGameBudgetTest {

    /** Always picks the first action, spending the given time on each decision. */
    static class SlowPlayer extends AbstractPlayer {
        final long thinkMillis;
        int decisions = 0;

        SlowPlayer(PlayerParameters params, long thinkMillis) {
            super(params, "SlowPlayer");
            this.thinkMillis = thinkMillis;
        }

        @Override
        public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
            spin(thinkMillis);
            decisions++;
            return possibleActions.get(0);
        }

        @Override
        public SlowPlayer copy() {
            return new SlowPlayer((PlayerParameters) parameters.copy(), thinkMillis);
        }
    }

    private static PlayerParameters gameSecondsParams(int seconds) {
        return clockParams(PlayerConstants.BUDGET_GAME_SECONDS, seconds);
    }

    private static PlayerParameters clockParams(PlayerConstants budgetType, int seconds) {
        PlayerParameters params = new PlayerParameters();
        params.setParameterValue("budgetType", budgetType);
        params.setParameterValue("budget", seconds);
        return params;
    }

    private Game ticTacToe(AbstractPlayer... players) {
        AbstractParameters params = new TicTacToeGameParameters();
        params.setRandomSeed(42);
        return new Game(GameType.TicTacToe, List.of(players), new TicTacToeForwardModel(),
                new TicTacToeGameState(params, players.length));
    }

    @Test
    public void gameBudgetIsConvertedFromSecondsToMillis() {
        assertEquals(5_000, gameSecondsParams(5).getClockBudgetMillis());
        assertEquals(3_000, clockParams(PlayerConstants.BUDGET_ROUND_SECONDS, 3).getClockBudgetMillis());
        PlayerParameters timeParams = new PlayerParameters();
        timeParams.setParameterValue("budgetType", PlayerConstants.BUDGET_TIME);
        timeParams.setParameterValue("budget", 5);
        assertEquals(-1, timeParams.getClockBudgetMillis());
    }

    @Test
    public void onlyGameSecondsPlayersHaveAGameTimer() {
        SlowPlayer budgeted = new SlowPlayer(gameSecondsParams(3), 0);
        RandomPlayer other = new RandomPlayer(new Random(1));
        ticTacToe(budgeted, other);
        assertNotNull(budgeted.getGameTimer());
        assertEquals(3_000, budgeted.getGameTimer().remainingTimeMillis());
        assertNull(other.getGameTimer());
    }

    @Test
    public void gameTimerIsDebitedByThinkingTimeAndRestoredOnReset() {
        SlowPlayer budgeted = new SlowPlayer(gameSecondsParams(10), 20);
        Game game = ticTacToe(budgeted, new RandomPlayer(new Random(2)));
        game.run();

        long remaining = budgeted.getGameTimer().remainingTimeMillis();
        assertTrue("decisions " + budgeted.decisions, budgeted.decisions > 1);
        // we allow a little slack for CPU-time granularity, but each decision spins for 20ms
        long minSpent = budgeted.decisions * 15L;
        assertTrue("remaining " + remaining, remaining <= 10_000 - minSpent);
        // and time outside _getAction (e.g. the opponent's moves) is not charged
        assertTrue("remaining " + remaining, remaining > 10_000 - budgeted.decisions * 20L - 500);

        game.reset(List.of(budgeted, new RandomPlayer(new Random(3))));
        assertEquals(10_000, budgeted.getGameTimer().remainingTimeMillis());
    }

    @Test
    public void gameTimerIsCreatedWhenPlayerIsUsedOutsideGame() {
        SlowPlayer budgeted = new SlowPlayer(gameSecondsParams(2), 10);
        Game game = ticTacToe(new RandomPlayer(new Random(1)), new RandomPlayer(new Random(2)));
        AbstractGameState state = game.getGameState();
        budgeted.setForwardModel(game.getForwardModel());
        budgeted.getAction(state, game.getForwardModel().computeAvailableActions(state));
        assertNotNull(budgeted.getGameTimer());
        assertTrue(budgeted.getGameTimer().remainingTimeMillis() < 2_000);
    }

    @Test
    public void roundTimerIsResetAtThePlayersFirstDecisionInEachRound() {
        SlowPlayer budgeted = new SlowPlayer(clockParams(PlayerConstants.BUDGET_ROUND_SECONDS, 2), 20);
        Game game = ticTacToe(new RandomPlayer(new Random(1)), new RandomPlayer(new Random(2)));
        AbstractGameState state = game.getGameState();
        budgeted.setForwardModel(game.getForwardModel());
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
        budgeted.getAction(state, actions);
        budgeted.getAction(state, actions);
        long afterTwo = budgeted.getGameTimer().remainingTimeMillis();
        assertTrue("remaining " + afterTwo, afterTwo <= 2_000 - 30);

        state.roundCounter = 1;
        budgeted.getAction(state, actions);
        long afterThree = budgeted.getGameTimer().remainingTimeMillis();
        // only the decision in the new round is charged
        assertTrue("remaining " + afterThree, afterThree > afterTwo);
        assertTrue("remaining " + afterThree, afterThree <= 2_000 - 15);
    }
}
