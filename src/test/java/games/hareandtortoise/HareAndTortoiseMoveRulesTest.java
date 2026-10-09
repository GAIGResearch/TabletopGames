package games.hareandtortoise;

import core.actions.AbstractAction;
import games.hareandtortoise.actions.ChewLettuce;
import games.hareandtortoise.actions.Move;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static org.junit.Assert.*;

/**
 * Forward moves (cost and legality) and lettuce squares. Positions are chosen so that the other rules (carrot squares,
 * moves back to a tortoise, number-square payouts, hare cards) cannot change the outcome: the runner under test
 * stands on START or squares 1-7 (no tortoise behind, not a carrot square), and no Move executed ends on a hare square.
 */
public class HareAndTortoiseMoveRulesTest {

    HareAndTortoiseParameters params;
    HareAndTortoiseGameState state;
    HareAndTortoiseForwardModel fm;

    @Before
    public void setup() {
        params = new HareAndTortoiseParameters();
        state = new HareAndTortoiseGameState(params, 3);
        fm = new HareAndTortoiseForwardModel();
        fm.setup(state);
    }

    @Test
    public void fromStartWith65CarrotsEveryAffordableNonTortoiseSquareIsOffered() {
        // 10 squares cost 55 (<= 65), 11 would cost 66; square 8 is a tortoise
        assertEquals(moves(0, 1, 2, 3, 4, 5, 6, 7, 9, 10), actions(fm, state));
    }

    @Test
    public void occupiedSquaresAreNotOffered() {
        place(state, 1, 3, 65, 3);
        place(state, 2, 5, 65, 3);
        assertEquals(moves(0, 1, 2, 4, 6, 7, 9, 10), actions(fm, state));
    }

    @Test
    public void squaresBeyondTheCarrotsHeldAreNotOffered() {
        // 10 carrots: 4 squares cost 1+2+3+4 = 10 (affordable), 5 squares cost 15
        place(state, 0, 0, 10, 3);
        assertEquals(moves(0, 1, 2, 3, 4), actions(fm, state));
    }

    @Test
    public void lettuceSquareIsNotOfferedWithNoLettuceLeft() {
        place(state, 0, 0, 65, 0);
        assertEquals(moves(0, 1, 2, 3, 4, 5, 6, 9, 10), actions(fm, state));
    }

    @Test
    public void onlyForwardMovesAreOfferedFromTheBoard() {
        // on square 4 with 10 carrots: 5 costs 1, 6 costs 3, 7 costs 6, 8 is a tortoise, 9 costs 15
        place(state, 0, 4, 10, 3);
        assertEquals(moves(4, 5, 6, 7), actions(fm, state));
    }

    @Test
    public void movePaysTheRaceCardCostAndPassesTheTurn() {
        fm.next(state, new Move(0, 4));
        assertEquals(4, state.getSquare(0));
        assertEquals(65 - 10, state.getCarrots(0));   // 65 - (1+2+3+4)
        assertEquals(3, state.getLettuces(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void landingOnALettuceSquareLeavesALettuceToChew() {
        fm.next(state, new Move(0, 7));
        assertEquals(7, state.getSquare(0));
        assertEquals(65 - 28, state.getCarrots(0));   // 65 - (1+2+...+7)
        assertTrue(state.hasLettuceToChew(0));
        assertEquals(3, state.getLettuces(0));        // chewed on the next turn, not on arrival
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void landingElsewhereLeavesNoLettuceToChew() {
        fm.next(state, new Move(0, 5));
        assertFalse(state.hasLettuceToChew(0));
    }

    @Test
    public void withALettuceToChewChewingIsTheOnlyAction() {
        place(state, 0, 7, 65, 3);
        state.lettuceToChew[0] = true;
        assertEquals(Set.<AbstractAction>of(new ChewLettuce()), actions(fm, state));
    }

    @Test
    public void chewingInFirstPlaceDiscardsALettuceAndDraws10() {
        place(state, 0, 7, 20, 3);
        state.lettuceToChew[0] = true;
        place(state, 1, 4, 65, 3);   // p0 leads: 1st
        fm.next(state, new ChewLettuce());
        assertEquals(7, state.getSquare(0));
        assertEquals(2, state.getLettuces(0));
        assertEquals(20 + 10, state.getCarrots(0));   // 10 x 1st
        assertFalse(state.hasLettuceToChew(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void chewingInSecondPlaceDraws20() {
        place(state, 0, 7, 20, 3);
        state.lettuceToChew[0] = true;
        place(state, 1, 10, 65, 3);  // p1 ahead: p0 is 2nd, p2 at START 3rd
        fm.next(state, new ChewLettuce());
        assertEquals(2, state.getLettuces(0));
        assertEquals(20 + 20, state.getCarrots(0));   // 10 x 2nd
    }

    @Test
    public void carrotsPerRacePositionParameterSetsTheChewPayout() {
        params.setParameterValue("carrotsPerRacePosition", 15);
        state = new HareAndTortoiseGameState(params, 3);
        fm.setup(state);
        place(state, 0, 7, 20, 3);
        state.lettuceToChew[0] = true;
        place(state, 1, 10, 65, 3);  // p0 2nd
        fm.next(state, new ChewLettuce());
        assertEquals(20 + 30, state.getCarrots(0));   // 15 x 2nd
    }

    @Test
    public void afterChewingOnlyMovesAreOffered() {
        // chewed last turn: lettuceToChew cleared, runner still on the lettuce square 7 with 2 lettuces left.
        // 20 carrots: 8 and 11 are tortoises, 9 is occupied, 10 costs 3, 12 costs 15, 13 would cost 21
        place(state, 0, 7, 20, 2);
        place(state, 1, 9, 65, 3);
        assertEquals(moves(7, 10, 12), actions(fm, state));
    }
}
