package games.hareandtortoise;

import core.Game;
import core.actions.AbstractAction;
import games.hareandtortoise.actions.ChewLettuce;
import games.hareandtortoise.actions.Move;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;
import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration tests: real games from the factory, driven only by fm.next. No executed Move ends on a hare square, and
 * no runner starts a turn on a number square that matches its race position, so the later phases' square effects do
 * not touch these scripts.
 */
public class HareAndTortoiseGameFlowTest {

    private static HareAndTortoiseGameState stateOf(Game game) {
        return (HareAndTortoiseGameState) game.getGameState();
    }

    private static HareAndTortoiseForwardModel fmOf(Game game) {
        return (HareAndTortoiseForwardModel) game.getForwardModel();
    }

    @Test
    public void lettuceIsChewedOnTheNextTurnAndThenTheRunnerMustMoveOn() {
        Game game = newGame(3, 11);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);

        fm.next(state, new Move(0, 7));             // lettuce square
        assertEquals(65 - 28, state.getCarrots(0));  // 65 - (1+2+...+7)
        assertTrue(state.hasLettuceToChew(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Move(0, 2));             // carrot square
        assertEquals(65 - 3, state.getCarrots(1));   // 65 - (1+2)
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getRoundCounter());
        fm.next(state, new Move(0, 5));             // carrot square
        assertEquals(65 - 15, state.getCarrots(2));  // 65 - (1+2+3+4+5)
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());

        assertEquals(Set.<AbstractAction>of(new ChewLettuce()), actions(fm, state));
        fm.next(state, new ChewLettuce());          // p0 on 7 leads p2 on 5 and p1 on 2: 1st
        assertEquals(37 + 10, state.getCarrots(0));
        assertEquals(2, state.getLettuces(0));
        assertEquals(7, state.getSquare(0));
        assertFalse(state.hasLettuceToChew(0));
        assertEquals(1, state.getCurrentPlayer());

        fm.next(state, new Move(2, 9));
        assertEquals(62 - 28, state.getCarrots(1));  // 62 - (1+2+...+7)
        fm.next(state, new Move(5, 10));
        assertEquals(50 - 15, state.getCarrots(2));  // 50 - (1+2+3+4+5)
        assertEquals(2, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());

        // 47 carrots on 7: 8, 11, 15 tortoises; 9, 10 occupied; 12 costs 15, 13 21, 14 28, 16 45, 17 would cost 55
        assertEquals(moves(7, 12, 13, 14, 16), actions(fm, state));
    }

    @Test
    public void playersHomeAreSkippedAndTheRoundWrapsToTheFirstRunnerStillRacing() {
        Game game = newGame(3, 12);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);
        place(state, 0, 60, 16, 0);

        fm.next(state, new Move(60, HOME_SQUARE));
        assertEquals(1, state.getFinishPosition(0));
        assertEquals(16 - 10, state.getCarrots(0));  // 16 - (1+2+3+4)
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Move(0, 2));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getRoundCounter());
        fm.next(state, new Move(0, 5));
        // p0 is home: the turn wraps round to p1, which ends the round
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Move(2, 9));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Move(5, 10));
        assertEquals(2, state.getRoundCounter());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void gameEndsWhenAllButOneAreHomeInFinishingOrder() {
        Game game = newGame(3, 13);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);
        place(state, 0, 60, 16, 0);
        place(state, 1, 59, 25, 0);
        place(state, 2, 21, 65, 3);

        fm.next(state, new Move(60, HOME_SQUARE));     // 16 - 10 = 6 <= 10
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(fm.computeAvailableActions(state).contains(new Move(59, HOME_SQUARE)));
        fm.next(state, new Move(59, HOME_SQUARE));     // 5 squares: 25 - 15 = 10 <= 20
        assertEquals(25 - 15, state.getCarrots(1));
        assertEquals(2, state.getFinishPosition(1));

        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
        assertEquals(3, state.getOrdinalPosition(2));
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
    }

    @Test
    public void theLastRunnerIsRankedLastAndOthersByFinishingPlace() {
        // 4 players: p3 and p1 are home; p2 gets home third, leaving p0 (the lowest index) as the last runner
        Game game = newGame(4, 14);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);
        putHome(state, 3, 1);
        putHome(state, 1, 2);
        place(state, 0, 21, 65, 3);
        place(state, 2, 60, 40, 0);
        state.setTurnOwner(2);

        fm.next(state, new Move(60, HOME_SQUARE));     // 40 - 10 = 30 <= 10 x (2 + 1)
        assertEquals(3, state.getFinishPosition(2));
        assertFalse(state.isNotTerminal());
        int[] expected = {4, 2, 3, 1};
        for (int p = 0; p < 4; p++)
            assertEquals(expected[p], state.getOrdinalPosition(p));
        assertEquals(WIN_GAME, state.getPlayerResults()[3]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
    }

    @Test
    public void aStuckRunnerGoesBackToStartWith65CarrotsAndMovesOffAtOnce() {
        Game game = newGame(3, 15);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);
        // p1 on hare square 6 with no carrots: no forward move is affordable and there is no tortoise square behind
        place(state, 1, 6, 0, 2);

        fm.next(state, new Move(0, 2));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getSquare(1));
        assertEquals(65, state.getCarrots(1));
        assertEquals(2, state.getLettuces(1));        // lettuces are kept
        assertFalse(state.hasLettuceToChew(1));
        assertFalse(state.missesNextTurn(1));
        // moves from START with 65 carrots, 2 occupied by p0
        assertEquals(moves(0, 1, 3, 4, 5, 6, 7, 9, 10), actions(fm, state));
    }

    @Test
    public void aRunnerWithAnAffordableMoveIsNotSentBack() {
        Game game = newGame(3, 16);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);
        place(state, 1, 6, 1, 2);                     // one carrot: 7 (lettuce, has lettuces) costs 1

        fm.next(state, new Move(0, 2));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(6, state.getSquare(1));
        assertEquals(1, state.getCarrots(1));
        assertEquals(moves(6, 7), actions(fm, state));
    }

    @Test
    public void maxRoundsEndsTheGameRankedByRacePosition() {
        HareAndTortoiseParameters params = new HareAndTortoiseParameters();
        params.setParameterValue("maxRounds", 2);
        Game game = newGame(3, 17, params);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);

        fm.next(state, new Move(0, 2));
        fm.next(state, new Move(0, 5));
        fm.next(state, new Move(0, 9));
        assertEquals(1, state.getRoundCounter());
        assertTrue(state.isNotTerminal());
        fm.next(state, new Move(2, 10));
        fm.next(state, new Move(5, 12));
        assertTrue(state.isNotTerminal());
        fm.next(state, new Move(9, 13));

        assertFalse(state.isNotTerminal());
        assertEquals(2, state.getRoundCounter());
        // p2 on 13, p1 on 12, p0 on 10
        assertEquals(3, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
        assertEquals(1, state.getOrdinalPosition(2));
        assertEquals(WIN_GAME, state.getPlayerResults()[2]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void seededRandomGamesRunToAnEndWithConsistentResults() {
        for (int nPlayers = 3; nPlayers <= 6; nPlayers++) {
            long seed = 100 + nPlayers;
            Game game = newGame(nPlayers, seed);
            HareAndTortoiseGameState state = stateOf(game);
            HareAndTortoiseForwardModel fm = fmOf(game);
            Random rnd = new Random(seed);
            int steps = 0;
            while (state.isNotTerminal() && steps++ < 20000) {
                int player = state.getCurrentPlayer();
                assertFalse("the player to move is home", state.isHome(player));
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertFalse("no action for player " + player, actions.isEmpty());
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
                for (int p = 0; p < nPlayers; p++) {
                    assertTrue(state.getCarrots(p) >= 0);
                    assertTrue(state.getLettuces(p) >= 0 && state.getLettuces(p) <= 3);
                    for (int q = p + 1; q < nPlayers; q++)
                        if (state.getSquare(p) > 0 && state.getSquare(p) < HOME_SQUARE)
                            assertNotEquals("two runners on one square", state.getSquare(p), state.getSquare(q));
                }
                assertEquals(12, state.getHareDeck().getSize());
            }
            assertFalse("game did not end within 20000 actions", state.isNotTerminal());
            assertTrue(steps > 1);

            int nHome = state.getNPlayersHome();
            // the game ends either with all but one home, or at the default 300-round limit
            assertTrue(nHome == nPlayers - 1 || state.getRoundCounter() == 300);
            if (nHome == nPlayers - 1) {
                for (int p = 0; p < nPlayers; p++)
                    assertEquals(state.isHome(p) ? state.getFinishPosition(p) : nPlayers, state.getOrdinalPosition(p));
            }
            int leaders = 0;
            for (int p = 0; p < nPlayers; p++)
                if (state.getOrdinalPosition(p) == 1) leaders++;
            assertEquals(1, leaders);
            for (int p = 0; p < nPlayers; p++)
                assertEquals(state.getOrdinalPosition(p) == 1 ? WIN_GAME : LOSE_GAME, state.getPlayerResults()[p]);
        }
    }
}
