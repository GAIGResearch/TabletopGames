package games.hareandtortoise;

import core.Game;
import games.hareandtortoise.actions.ChewCarrot;
import games.hareandtortoise.actions.Move;
import games.hareandtortoise.components.HareCard;
import org.junit.Test;

import java.util.List;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static games.hareandtortoise.components.HareCard.Type.*;
import static org.junit.Assert.*;

/**
 * Integration: a scripted 3-player game from the factory with a stacked hare deck, driven by fm.next, going through
 * another turn, the draw-or-discard choice, a missed turn and a card move back onto a carrot square.
 */
public class HareAndTortoiseHareCardFlowTest {

    @Test
    public void scriptedGameThroughFourHareCards() {
        Game game = newGame(3, 31);
        HareAndTortoiseGameState state = stateOf(game);
        HareAndTortoiseForwardModel fm = fmOf(game);
        stackHareDeck(state, ANOTHER_TURN, DRAW_OR_DISCARD_10, MISS_A_TURN, PREVIOUS_CARROT_SQUARE);
        List<HareCard.Type> start = hareTypes(state);

        play(fm, state, new Move(0, 1));             // p0: hare, ANOTHER_TURN
        assertEquals(65 - 1, state.getCarrots(0));
        assertEquals(0, state.getCurrentPlayer());
        play(fm, state, new Move(1, 3));             // p0 again: hare, DRAW_OR_DISCARD_10
        assertEquals(64 - 3, state.getCarrots(0));   // 1+2
        assertEquals(0, state.getCurrentPlayer());
        play(fm, state, new ChewCarrot(true));
        assertEquals(61 + 10, state.getCarrots(0));
        assertEquals(1, state.getCurrentPlayer());

        play(fm, state, new Move(0, 6));             // p1: hare, MISS_A_TURN
        assertEquals(65 - 21, state.getCarrots(1));
        assertTrue(state.missesNextTurn(1));
        assertEquals(2, state.getCurrentPlayer());

        play(fm, state, new Move(0, 2));             // p2: carrot square, no card
        assertEquals(65 - 3, state.getCarrots(2));
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());

        play(fm, state, new Move(3, 5));             // p0: carrot square; p1 is skipped
        assertEquals(71 - 3, state.getCarrots(0));
        assertEquals(2, state.getCurrentPlayer());
        assertFalse(state.missesNextTurn(1));
        assertEquals(44, state.getCarrots(1));
        assertEquals(6, state.getSquare(1));
        assertEquals(1, state.getRoundCounter());

        play(fm, state, new Move(2, 3));             // p2: hare, PREVIOUS_CARROT_SQUARE -> back to 2, just vacated
        assertEquals(2, state.getSquare(2));
        assertEquals(62 - 1, state.getCarrots(2));
        assertEquals(2, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());

        assertEquals(topToBottom(start, 4), hareTypes(state));
        assertEquals(12 - 4, state.getNUnseenHareCards());
        // p0 on carrot square 5 may chew there
        assertTrue(actions(fm, state).contains(new ChewCarrot(true)));
        assertTrue(actions(fm, state).contains(new ChewCarrot(false)));
    }
}
