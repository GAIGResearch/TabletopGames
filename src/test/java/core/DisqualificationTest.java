package core;

import core.GameClockTest.SlowPlayer;
import games.GameType;
import games.loveletter.LoveLetterParameters;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import org.junit.Test;

import java.util.List;
import java.util.function.Function;

import static core.CoreConstants.GameResult.*;
import static org.junit.Assert.*;

public class DisqualificationTest {

    private static CoreParameters disqualifyOnTimeout() {
        CoreParameters core = new CoreParameters();
        core.disqualifyPlayerOnTimeout = true;
        return core;
    }

    private Game ticTacToe(long thinkingTimeMillis, AbstractPlayer... players) {
        AbstractParameters params = new TicTacToeGameParameters();
        params.setRandomSeed(42);
        params.setThinkingTimeMillis(thinkingTimeMillis);
        Game game = new Game(GameType.TicTacToe, List.of(players), new TicTacToeForwardModel(),
                new TicTacToeGameState(params, players.length));
        game.setCoreParameters(disqualifyOnTimeout());
        return game;
    }

    private Game loveLetter(long seed, long thinkingTimeMillis, AbstractPlayer... players) {
        LoveLetterParameters params = new LoveLetterParameters();
        params.setThinkingTimeMillis(thinkingTimeMillis);
        Game game = GameType.LoveLetter.createGameInstance(players.length, seed, params);
        game.reset(List.of(players));
        game.setCoreParameters(disqualifyOnTimeout());
        return game;
    }

    @Test
    public void timeoutDisqualificationEndsATwoPlayerGame() {
        SlowPlayer slow = new SlowPlayer(150);
        SlowPlayer opponent = new SlowPlayer(0);
        Game game = ticTacToe(100, slow, opponent);
        game.run();

        AbstractGameState state = game.getGameState();
        assertEquals(GAME_END, state.getGameStatus());
        assertArrayEquals(new CoreConstants.GameResult[]{DISQUALIFY, WIN_GAME}, state.getPlayerResults());
        assertEquals("no action is taken for the disqualified player", 0, state.getHistory().size());
        assertTrue("opponent never moves", opponent.chosen.isEmpty());
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(1, state.getOrdinalPosition(1));
    }

    @Test
    public void disqualifiedPlayerMakesNoMoreDecisionsAndStillLoses() {
        // player 2 overruns its clock on its first decision; the other two keep playing
        for (long seed = 1; seed <= 3; seed++) {
            SlowPlayer slow = new SlowPlayer(350);
            Game game = loveLetter(seed, 300, new SlowPlayer(0), new SlowPlayer(0), slow);
            game.run();

            AbstractGameState state = game.getGameState();
            assertEquals(1, slow.chosen.size());
            assertEquals(GAME_END, state.getGameStatus());
            assertEquals(DISQUALIFY, state.getPlayerResults()[2]);
            assertEquals(3, state.getOrdinalPosition(2));
            // the random actions taken for player 2 can end the game (with player 2 top on score); the others are
            // still ranked above them
            assertFalse(state.getWinners().isEmpty() && state.getTied().isEmpty());
            assertFalse(state.getWinners().contains(2) || state.getTied().contains(2));
        }
    }

    @Test
    public void gameContinuesUntilOnlyOneTeamIsLeft() {
        Game game = GameType.LoveLetter.createGameInstance(3, 1);
        AbstractGameState state = game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        fm.disqualify(state, 1);
        assertTrue(state.isNotTerminal());
        assertFalse(state.isDecidedByDisqualification());
        assertEquals(DISQUALIFY, state.getPlayerResults()[1]);

        fm.disqualify(state, 2);
        assertFalse(state.isNotTerminal());
        assertTrue(state.isDecidedByDisqualification());
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, DISQUALIFY, DISQUALIFY}, state.getPlayerResults());
    }

    @Test
    public void disqualificationIsNotOverwritten() {
        AbstractGameState state = GameType.LoveLetter.createGameInstance(3, 1).getGameState();
        state.setPlayerResult(DISQUALIFY, 0);
        state.setPlayerResult(WIN_GAME, 0);
        assertEquals(DISQUALIFY, state.getPlayerResults()[0]);
    }

    @Test
    public void disqualifiedPlayersAreRankedLast() {
        AbstractGameState state = GameType.TicTacToe.createGameInstance(2, 1).getGameState();
        // player 0 has the better score, so is ranked last only because of the disqualification
        Function<Integer, Double> score = p -> p == 0 ? 10.0 : 0.0;
        assertEquals(1, state.getOrdinalPosition(0, score, null));
        state.setPlayerResult(DISQUALIFY, 0);
        assertEquals(2, state.getOrdinalPosition(0, score, null));
        assertEquals(1, state.getOrdinalPosition(1, score, null));
    }
}
