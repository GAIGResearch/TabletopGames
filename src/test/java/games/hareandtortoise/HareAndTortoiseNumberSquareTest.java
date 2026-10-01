package games.hareandtortoise;

import core.actions.AbstractAction;
import games.hareandtortoise.actions.ChewCarrot;
import games.hareandtortoise.actions.Move;
import org.junit.Test;

import java.util.Set;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static org.junit.Assert.*;

/**
 * Number-square payouts, and the stuck check that follows them. Each test arranges the positions and has the previous
 * player act, so the payout is made as the turn passes to the runner under test. Nothing lands on a hare square.
 */
public class HareAndTortoiseNumberSquareTest {

    private static HareAndTortoiseGameState newState(int nPlayers, HareAndTortoiseParameters params) {
        HareAndTortoiseGameState state = new HareAndTortoiseGameState(params, nPlayers);
        new HareAndTortoiseForwardModel().setup(state);
        return state;
    }

    private final HareAndTortoiseForwardModel fm = new HareAndTortoiseForwardModel();

    @Test
    public void secondPlaceOnATwoSquareDraws20AtTheStartOfTheTurn() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 10, 20, 3);    // "2" square
        place(state, 2, 12, 65, 3);    // ahead of p1: p1 2nd
        fm.next(state, new Move(0, 2)); // p0 stays behind
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(20 + 20, state.getCarrots(1));   // 10 x 2nd
        assertEquals(10, state.getSquare(1));
        assertEquals(65 - 3, state.getCarrots(0));    // the mover is not paid: 65 - (1+2)
        assertEquals(65, state.getCarrots(2));
    }

    @Test
    public void firstPlaceOnATwoSquareDrawsNothing() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 10, 20, 3);
        place(state, 2, 5, 65, 3);     // behind p1: p1 1st
        fm.next(state, new Move(0, 2));
        assertEquals(20, state.getCarrots(1));
    }

    @Test
    public void thirdPlaceOnAThreeSquareDraws30() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 0, 17, 20, 3);
        place(state, 1, 12, 20, 3);    // "3" square
        place(state, 2, 13, 65, 3);
        fm.next(state, new Move(17, 18)); // p0 and p2 ahead: p1 3rd
        assertEquals(20 + 30, state.getCarrots(1));   // 10 x 3rd
    }

    @Test
    public void fourthPlaceOnAFourSquareDraws40() {
        HareAndTortoiseGameState state = newState(4, new HareAndTortoiseParameters());
        place(state, 0, 17, 20, 3);
        place(state, 1, 9, 20, 3);     // "4" square
        place(state, 2, 12, 65, 3);
        place(state, 3, 13, 65, 3);
        fm.next(state, new Move(17, 18)); // three ahead: p1 4th
        assertEquals(20 + 40, state.getCarrots(1));   // 10 x 4th
    }

    @Test
    public void firstPlaceOnAFlagDraws10() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 16, 30, 3);    // flag; 22 (6 squares, 21) is open, so never stuck
        place(state, 2, 12, 65, 3);
        fm.next(state, new Move(0, 2));
        assertEquals(30 + 10, state.getCarrots(1));   // 10 x 1st
    }

    @Test
    public void fifthPlaceOnAFlagDraws50() {
        HareAndTortoiseGameState state = newState(6, new HareAndTortoiseParameters());
        place(state, 1, 16, 30, 3);    // flag; 22 (6 squares, 21) is open, so never stuck
        place(state, 2, 17, 65, 3);
        place(state, 3, 18, 65, 3);
        place(state, 4, 20, 65, 3);
        place(state, 5, 21, 65, 3);
        fm.next(state, new Move(0, 2)); // p0 stays behind: p1 5th
        assertEquals(30 + 50, state.getCarrots(1));   // 10 x 5th
    }

    @Test
    public void sixthPlaceOnAFlagDraws60() {
        HareAndTortoiseGameState state = newState(6, new HareAndTortoiseParameters());
        place(state, 0, 23, 65, 3);
        place(state, 1, 16, 30, 3);    // flag; 22 (6 squares, 21) is open, so never stuck
        place(state, 2, 17, 65, 3);
        place(state, 3, 18, 65, 3);
        place(state, 4, 20, 65, 3);
        place(state, 5, 21, 65, 3);
        fm.next(state, new Move(23, 27)); // everyone ahead: p1 6th
        assertEquals(30 + 60, state.getCarrots(1));   // 10 x 6th
    }

    @Test
    public void secondPlaceOnAFlagDrawsNothing() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 16, 20, 3);
        place(state, 2, 17, 65, 3);    // p1 2nd
        fm.next(state, new Move(0, 2));
        assertEquals(20, state.getCarrots(1));
    }

    @Test
    public void runnersHomeCountAheadForThePayout() {
        HareAndTortoiseGameState state = newState(4, new HareAndTortoiseParameters());
        putHome(state, 3, 1);
        place(state, 1, 10, 20, 3);    // "2" square; nobody on the board ahead, but p3 is home: p1 2nd
        fm.next(state, new Move(0, 2));
        assertEquals(20 + 20, state.getCarrots(1));   // 10 x 2nd
    }

    @Test
    public void thePositionWhenTheTurnStartsCountsNotWhenTheRunnerLanded() {
        // p1 landed on "2" square 10 while 2nd, but p0 overtakes before p1's turn: 3rd, nothing paid
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 0, 5, 65, 3);
        place(state, 1, 10, 20, 3);
        place(state, 2, 12, 65, 3);
        fm.next(state, new Move(5, 13));
        assertEquals(20, state.getCarrots(1));
    }

    @Test
    public void beingOvertakenOntoTheMatchingPositionPays() {
        // p1 on "3" square 12 is 2nd; p0 overtakes from 9 to 17, so p1 starts the turn 3rd
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 0, 9, 65, 3);
        place(state, 1, 12, 20, 3);
        place(state, 2, 13, 65, 3);
        fm.next(state, new Move(9, 17));
        assertEquals(65 - 36, state.getCarrots(0));   // 65 - (1+2+...+8)
        assertEquals(20 + 30, state.getCarrots(1));   // 10 x 3rd
    }

    @Test
    public void nothingIsPaidForLandingOnANumberSquare() {
        // p0 lands on "2" square 10 behind p1 on 12: 2nd, but pays 1+2+...+10 and draws nothing
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 12, 20, 3);
        fm.next(state, new Move(0, 10));
        assertEquals(65 - 55, state.getCarrots(0));
        assertEquals(20, state.getCarrots(1));         // p1 is 1st on a "3": nothing
    }

    @Test
    public void thePayoutIsMadeOncePerTurnStart() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 10, 20, 3);
        place(state, 2, 12, 65, 3);
        fm.next(state, new Move(0, 2));
        fm.computeAvailableActions(state);
        fm.computeAvailableActions(state);
        assertEquals(20 + 20, state.getCarrots(1));
        // p1 moves on 10 -> 13 (1+2+3 = 6); p2 on "3" square 12 is then 2nd: nothing
        fm.next(state, new Move(10, 13));
        assertEquals(40 - 6, state.getCarrots(1));
        assertEquals(65, state.getCarrots(2));
    }

    @Test
    public void thePayoutIsMadeWhenTheTurnWrapsToTheFirstPlayer() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 0, 10, 20, 3);    // "2" square
        place(state, 1, 12, 65, 3);    // p0 2nd
        state.setTurnOwner(2);
        fm.next(state, new Move(0, 2));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(20 + 20, state.getCarrots(0));   // 10 x 2nd
    }

    @Test
    public void carrotsPerRacePositionParameterSetsTheNumberSquarePayout() {
        HareAndTortoiseParameters params = new HareAndTortoiseParameters();
        params.setParameterValue("carrotsPerRacePosition", 15);
        HareAndTortoiseGameState state = newState(3, params);
        place(state, 1, 10, 20, 3);
        place(state, 2, 12, 65, 3);
        fm.next(state, new Move(0, 2));
        assertEquals(20 + 30, state.getCarrots(1));   // 15 x 2nd
    }

    // ---------------------------------------------------------------- the stuck check

    @Test
    public void thePayoutComesBeforeTheStuckCheckAndCanSaveARunner() {
        // p1 on "3" square 4 with no carrots and no tortoise behind; 3rd after p0's move -> draws 30 and can move
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 0, 2, 65, 3);
        place(state, 1, 4, 0, 3);
        place(state, 2, 9, 65, 3);
        fm.next(state, new Move(2, 5));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(4, state.getSquare(1));
        assertEquals(0 + 30, state.getCarrots(1));    // 10 x 3rd
        // 30 carrots: 7 squares cost 28; 5 and 9 occupied, 8 and 11 tortoises
        assertEquals(moves(4, 6, 7, 10), actions(fm, state));
    }

    @Test
    public void aRunnerOnACarrotSquareWithNoCarrotsIsNotStuck() {
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 5, 0, 3);
        fm.next(state, new Move(0, 2));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(5, state.getSquare(1));
        assertEquals(0, state.getCarrots(1));
        assertEquals(Set.<AbstractAction>of(new ChewCarrot(true)), actions(fm, state));
    }

    @Test
    public void aRunnerWithOnlyTheTortoiseMoveIsNotStuck() {
        // p1 on "4" square 9 in 1st place (no payout) with no carrots: only the move back to 8
        HareAndTortoiseGameState state = newState(3, new HareAndTortoiseParameters());
        place(state, 1, 9, 0, 3);
        fm.next(state, new Move(0, 2));
        assertEquals(9, state.getSquare(1));
        assertEquals(0, state.getCarrots(1));
        assertEquals(moves(9, 8), actions(fm, state));
    }
}
