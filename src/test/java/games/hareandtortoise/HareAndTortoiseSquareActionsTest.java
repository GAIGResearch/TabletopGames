package games.hareandtortoise;

import core.actions.AbstractAction;
import games.hareandtortoise.actions.ChewCarrot;
import games.hareandtortoise.actions.ChewLettuce;
import games.hareandtortoise.actions.Move;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static org.junit.Assert.*;

/**
 * Carrot squares and tortoise squares. Player 0 is the player to move; the others stay on START unless placed. No Move
 * executed here ends on a hare square, and nobody the turn passes to starts on a number square matching their position.
 */
public class HareAndTortoiseSquareActionsTest {

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

    private static Set<AbstractAction> with(Set<AbstractAction> set, AbstractAction... more) {
        set.addAll(Set.of(more));
        return set;
    }

    // ---------------------------------------------------------------- carrot squares

    @Test
    public void onACarrotSquareBothChewsAreOfferedAlongsideTheMoves() {
        // 65 carrots on 5: 10 squares cost 55 (15 would be 11 squares = 66); 8, 11, 15 tortoises; no tortoise behind 5
        place(state, 0, 5, 65, 3);
        assertEquals(with(moves(5, 6, 7, 9, 10, 12, 13, 14), new ChewCarrot(true), new ChewCarrot(false)),
                actions(fm, state));
    }

    @Test
    public void payingTenIsOfferedWithExactlyTenCarrots() {
        // 10 carrots on 2: 4 squares cost 1+2+3+4 = 10 -> 3, 4, 5, 6
        place(state, 0, 2, 10, 3);
        assertEquals(with(moves(2, 3, 4, 5, 6), new ChewCarrot(true), new ChewCarrot(false)), actions(fm, state));
    }

    @Test
    public void payingTenIsNotOfferedWithNineCarrots() {
        // 9 carrots on 2: 3 squares cost 6, 4 would cost 10 -> 3, 4, 5
        place(state, 0, 2, 9, 3);
        assertEquals(with(moves(2, 3, 4, 5), new ChewCarrot(true)), actions(fm, state));
    }

    @Test
    public void withNoCarrotsOnACarrotSquareDrawingIsTheOnlyAction() {
        place(state, 0, 5, 0, 3);
        assertEquals(Set.<AbstractAction>of(new ChewCarrot(true)), actions(fm, state));
    }

    @Test
    public void drawingOnACarrotSquareAdds10AndTheRunnerStays() {
        place(state, 0, 5, 20, 3);
        fm.next(state, new ChewCarrot(true));
        assertEquals(5, state.getSquare(0));
        assertEquals(20 + 10, state.getCarrots(0));
        assertEquals(3, state.getLettuces(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void payingOnACarrotSquareRemoves10AndTheRunnerStays() {
        place(state, 0, 5, 20, 3);
        fm.next(state, new ChewCarrot(false));
        assertEquals(5, state.getSquare(0));
        assertEquals(20 - 10, state.getCarrots(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void carrotsPerChewParameterSetsTheAmountDrawnAndPaid() {
        params.setParameterValue("carrotsPerChew", 15);
        state = new HareAndTortoiseGameState(params, 3);
        fm.setup(state);
        // 14 carrots on 2: pay 15 is not offered; 4 squares cost 10, 5 would cost 15 -> 3, 4, 5, 6
        place(state, 0, 2, 14, 3);
        assertEquals(with(moves(2, 3, 4, 5, 6), new ChewCarrot(true)), actions(fm, state));
        fm.next(state, new ChewCarrot(true));
        assertEquals(14 + 15, state.getCarrots(0));
    }

    // ---------------------------------------------------------------- tortoise squares

    @Test
    public void fromANonCarrotSquareTheMoveBackToTheNearestTortoiseIsOfferedButNoChew() {
        // 3 carrots on 12 (a "3" square): forward 13 costs 1, 14 costs 3; tortoise 11 is the nearest behind
        place(state, 0, 12, 3, 3);
        assertEquals(moves(12, 13, 14, 11), actions(fm, state));
    }

    @Test
    public void fromACarrotSquareTheTortoiseMoveAndBothChewsAreOffered() {
        // 10 carrots on 26: 27 costs 1, 28 3, 29 6, 30 is a tortoise; tortoise 24 is the nearest behind
        place(state, 0, 26, 10, 3);
        assertEquals(with(moves(26, 27, 28, 29, 24), new ChewCarrot(true), new ChewCarrot(false)),
                actions(fm, state));
    }

    @Test
    public void anOccupiedNearestTortoiseBlocksTheMoveBackEvenIfAnEarlierOneIsFree() {
        place(state, 0, 12, 3, 3);
        place(state, 1, 11, 65, 3);    // nearest tortoise taken; 8 is free but may not be skipped to
        assertEquals(moves(12, 13, 14), actions(fm, state));
    }

    @Test
    public void thereIsNoTortoiseMoveFromTheFirstTortoiseSquare() {
        // 3 carrots on 8: 9 costs 1, 10 costs 3
        place(state, 0, 8, 3, 3);
        assertEquals(moves(8, 9, 10), actions(fm, state));
    }

    @Test
    public void aLettuceToChewStillLeavesOnlyChewLettuceEvenWithATortoiseBehind() {
        place(state, 0, 22, 65, 3);
        state.lettuceToChew[0] = true;
        assertEquals(Set.<AbstractAction>of(new ChewLettuce()), actions(fm, state));
    }

    @Test
    public void movingBackOneSquareToATortoiseDraws10() {
        place(state, 0, 12, 3, 3);
        fm.next(state, new Move(12, 11));
        assertEquals(11, state.getSquare(0));
        assertEquals(3 + 10, state.getCarrots(0));     // 10 x 1 square
        assertEquals(3, state.getLettuces(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void movingBackTwoSquaresToATortoiseDraws20() {
        place(state, 0, 10, 0, 3);
        fm.next(state, new Move(10, 8));
        assertEquals(8, state.getSquare(0));
        assertEquals(0 + 20, state.getCarrots(0));     // 10 x 2 squares
    }

    @Test
    public void movingBackFromOneTortoiseToThePreviousDraws10PerSquare() {
        // 11 -> 8 is 3 squares; 8 is the nearest tortoise behind 11
        place(state, 0, 11, 5, 3);
        assertTrue(actions(fm, state).contains(new Move(11, 8)));
        fm.next(state, new Move(11, 8));
        assertEquals(8, state.getSquare(0));
        assertEquals(5 + 30, state.getCarrots(0));     // 10 x 3 squares
    }

    @Test
    public void movingBackFiveSquaresDraws50() {
        // 29 -> 24 (nearest tortoise behind 29)
        place(state, 0, 29, 7, 3);
        assertTrue(actions(fm, state).contains(new Move(29, 24)));
        fm.next(state, new Move(29, 24));
        assertEquals(24, state.getSquare(0));
        assertEquals(7 + 50, state.getCarrots(0));     // 10 x 5 squares
    }

    @Test
    public void carrotsPerTortoiseStepParameterSetsTheAmountDrawn() {
        params.setParameterValue("carrotsPerTortoiseStep", 5);
        state = new HareAndTortoiseGameState(params, 3);
        fm.setup(state);
        place(state, 0, 10, 0, 3);
        fm.next(state, new Move(10, 8));
        assertEquals(0 + 10, state.getCarrots(0));     // 5 x 2 squares
    }
}
