package games.hareandtortoise;

import games.hareandtortoise.actions.Move;
import org.junit.Before;
import org.junit.Test;

import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;
import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static org.junit.Assert.*;

/**
 * Reaching HOME. Player 0 stands on square 60 (a "2" square); square 62 is a lettuce and 61 and 63 are hare squares.
 * Another runner is parked on 56, the tortoise square behind 60, so the later rule allowing a move back to an
 * unoccupied tortoise square adds nothing to these action sets.
 * HOME from 60 is 4 squares: 1+2+3+4 = 10 carrots. 61 costs 1, 63 costs 1+2+3 = 6.
 */
public class HareAndTortoiseHomeTest {

    HareAndTortoiseParameters params;
    HareAndTortoiseGameState state;
    HareAndTortoiseForwardModel fm;

    private void newState(int nPlayers) {
        state = new HareAndTortoiseGameState(params, nPlayers);
        fm = new HareAndTortoiseForwardModel();
        fm.setup(state);
    }

    @Before
    public void setup() {
        params = new HareAndTortoiseParameters();
        newState(3);
    }

    @Test
    public void firstHomeMayHoldUpTo10CarrotsAfterPaying() {
        place(state, 1, 56, 65, 3);
        place(state, 0, 60, 16, 0);   // 16 - 10 = 6 <= 10
        assertEquals(moves(60, 61, 63, HOME_SQUARE), actions(fm, state));
        place(state, 0, 60, 20, 0);   // 20 - 10 = 10 <= 10
        assertEquals(moves(60, 61, 63, HOME_SQUARE), actions(fm, state));
        place(state, 0, 60, 21, 0);   // 21 - 10 = 11 > 10
        assertEquals(moves(60, 61, 63), actions(fm, state));
    }

    @Test
    public void homeNeedsNoLettucesLeft() {
        place(state, 1, 56, 65, 3);
        place(state, 0, 60, 16, 1);   // with a lettuce, the lettuce square 62 (cost 1+2 = 3) is open but HOME is not
        assertEquals(moves(60, 61, 62, 63), actions(fm, state));
    }

    @Test
    public void homeMustBeAffordable() {
        place(state, 1, 56, 65, 3);
        place(state, 0, 60, 9, 0);    // HOME costs 10 > 9
        assertEquals(moves(60, 61, 63), actions(fm, state));
    }

    @Test
    public void secondHomeMayHoldUpTo20() {
        putHome(state, 2, 1);
        place(state, 1, 56, 65, 3);
        place(state, 0, 60, 30, 0);   // 30 - 10 = 20 <= 10 x (1 + 1)
        assertEquals(moves(60, 61, 63, HOME_SQUARE), actions(fm, state));
        place(state, 0, 60, 31, 0);   // 31 - 10 = 21 > 20
        assertEquals(moves(60, 61, 63), actions(fm, state));
    }

    @Test
    public void thirdHomeMayHoldUpTo30() {
        newState(4);
        putHome(state, 1, 1);
        putHome(state, 2, 2);
        place(state, 3, 56, 65, 3);
        place(state, 0, 60, 40, 0);   // 40 - 10 = 30 <= 10 x (2 + 1)
        assertEquals(moves(60, 61, 63, HOME_SQUARE), actions(fm, state));
        place(state, 0, 60, 41, 0);   // 41 - 10 = 31 > 30
        assertEquals(moves(60, 61, 63), actions(fm, state));
    }

    @Test
    public void homeCarrotsPerRacePositionParameterSetsTheLimit() {
        params.setParameterValue("homeCarrotsPerRacePosition", 20);
        newState(3);
        place(state, 1, 56, 65, 3);
        place(state, 0, 60, 21, 0);   // 21 - 10 = 11 <= 20 x 1
        assertEquals(moves(60, 61, 63, HOME_SQUARE), actions(fm, state));
    }

    @Test
    public void reachingHomeGivesTheNextFinishingPlace() {
        newState(4);
        putHome(state, 2, 1);
        place(state, 1, 56, 65, 3);
        place(state, 0, 60, 16, 0);
        fm.next(state, new Move(60, HOME_SQUARE));
        assertEquals(HOME_SQUARE, state.getSquare(0));
        assertEquals(16 - 10, state.getCarrots(0));   // 16 - (1+2+3+4)
        assertEquals(2, state.getFinishPosition(0));   // one player already home
        assertTrue(state.isHome(0));
        assertEquals(2, state.getRacePosition(0));
        assertTrue(state.isNotTerminal());            // two of four still racing
        assertEquals(1, state.getCurrentPlayer());
    }
}
