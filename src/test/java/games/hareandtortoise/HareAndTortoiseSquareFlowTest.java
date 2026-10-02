package games.hareandtortoise;

import core.Game;
import games.hareandtortoise.actions.ChewCarrot;
import games.hareandtortoise.actions.Move;
import org.junit.Test;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: a scripted 3-player game from the factory using carrot squares, tortoise squares and a number-square
 * payout, driven only by fm.next. No Move ends on a hare square.
 */
public class HareAndTortoiseSquareFlowTest {

    @Test
    public void carrotChewsTortoiseMovesAndANumberPayoutInARealGame() {
        Game game = newGame(3, 21);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);

        // round 0
        play(fm, state, new Move(0, 5));              // carrot square
        assertEquals(65 - 15, state.getCarrots(0));    // 65 - (1+...+5)
        play(fm, state, new Move(0, 2));              // carrot square
        assertEquals(65 - 3, state.getCarrots(1));     // 65 - (1+2)
        play(fm, state, new Move(0, 4));              // "3" square
        assertEquals(65 - 10, state.getCarrots(2));    // 65 - (1+...+4)

        // round 1
        assertEquals(0, state.getCurrentPlayer());
        play(fm, state, new ChewCarrot(true));        // stays on 5
        assertEquals(5, state.getSquare(0));
        assertEquals(50 + 10, state.getCarrots(0));
        play(fm, state, new ChewCarrot(false));       // stays on 2
        assertEquals(2, state.getSquare(1));
        assertEquals(62 - 10, state.getCarrots(1));
        // p2 starts on "3" square 4 in 2nd place (p0 on 5 ahead, p1 on 2 behind): nothing paid
        assertEquals(55, state.getCarrots(2));
        play(fm, state, new Move(4, 9));
        assertEquals(55 - 15, state.getCarrots(2));    // 55 - (1+...+5)

        // round 2
        play(fm, state, new ChewCarrot(true));        // a second chew on consecutive turns
        assertEquals(5, state.getSquare(0));
        assertEquals(60 + 10, state.getCarrots(0));
        play(fm, state, new Move(2, 10));
        assertEquals(52 - 36, state.getCarrots(1));    // 52 - (1+...+8)
        // p2 starts on "4" square 9 in 2nd place (p1 on 10 ahead): nothing paid
        assertEquals(40, state.getCarrots(2));
        play(fm, state, new Move(9, 12));
        assertEquals(40 - 6, state.getCarrots(2));     // 40 - (1+2+3)

        // round 3
        play(fm, state, new Move(5, 13));
        assertEquals(70 - 36, state.getCarrots(0));    // 70 - (1+...+8)
        // p1 starts on "2" square 10 in 3rd place (13, 12 ahead): nothing paid
        assertEquals(16, state.getCarrots(1));
        play(fm, state, new Move(10, 8));             // back 2 squares to the tortoise
        assertEquals(8, state.getSquare(1));
        assertEquals(16 + 20, state.getCarrots(1));
        // p2 starts on "3" square 12 in 2nd place: nothing paid
        assertEquals(34, state.getCarrots(2));
        play(fm, state, new Move(12, 11));            // back 1 square to the tortoise
        assertEquals(34 + 10, state.getCarrots(2));

        // round 4
        play(fm, state, new ChewCarrot(false));       // p0 on carrot square 13
        assertEquals(34 - 10, state.getCarrots(0));
        play(fm, state, new Move(8, 10));             // no tortoise behind 8, so forward
        assertEquals(36 - 3, state.getCarrots(1));     // 36 - (1+2)
        play(fm, state, new Move(11, 8));             // from one tortoise to the previous: 3 squares
        assertEquals(8, state.getSquare(2));
        assertEquals(44 + 30, state.getCarrots(2));

        // round 5
        play(fm, state, new Move(13, 16));            // flag, landing pays nothing
        assertEquals(24 - 6, state.getCarrots(0));     // 24 - (1+2+3)
        // p1 starts on "2" square 10 in 2nd place (p0 on 16 ahead, p2 on 8 behind): draws 20
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(33 + 20, state.getCarrots(1));
        assertEquals(5, state.getRoundCounter());
    }
}
