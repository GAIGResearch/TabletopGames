package games.hareandtortoise;

import core.actions.AbstractAction;
import games.hareandtortoise.actions.ChewCarrot;
import games.hareandtortoise.actions.Move;
import games.hareandtortoise.components.HareCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.hareandtortoise.HareAndTortoiseTestUtils.*;
import static games.hareandtortoise.components.HareCard.Type.*;
import static org.junit.Assert.*;

/**
 * The hare cards. Unit states with 4 players; p0 moves unless stated, and the others wait at START, which never counts
 * as "behind" or "ahead".
 */
public class HareAndTortoiseHareCardTest {

    HareAndTortoiseGameState state;
    final HareAndTortoiseForwardModel fm = new HareAndTortoiseForwardModel();

    @Before
    public void setup() {
        state = new HareAndTortoiseGameState(new HareAndTortoiseParameters(), 4);
        fm.setup(state);
    }

    // ---------------------------------------------------------------- the draw

    @Test
    public void aPaidForwardMoveOntoAHareSquareDrawsTheTopCardAndPutsItAtTheBottom() {
        stackHareDeck(state, LAST_TURN_FREE);
        List<HareCard.Type> before = hareTypes(state);

        play(fm, state, new Move(0, 1));
        assertEquals(topToBottom(before, 1), hareTypes(state));
        assertEquals(LAST_TURN_FREE, hareTypes(state).get(11));
        assertEquals(12, state.getHareDeck().getSize());
        assertEquals(12 - 1, state.getNUnseenHareCards());
        assertEquals(1, state.getSquare(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void theUnseenCountFallsByOnePerDrawAndStopsAtZero() {
        stackHareDeck(state, LAST_TURN_FREE, LAST_TURN_FREE);
        List<HareCard.Type> before = hareTypes(state);
        state.nUnseenHareCards = 1;

        play(fm, state, new Move(0, 1));   // p0
        assertEquals(0, state.getNUnseenHareCards());
        play(fm, state, new Move(0, 3));   // p1
        assertEquals(0, state.getNUnseenHareCards());
        assertEquals(topToBottom(before, 2), hareTypes(state));
    }

    @Test
    public void aMoveOntoAnyOtherSquareDrawsNoCard() {
        List<HareCard.Type> before = hareTypes(state);
        play(fm, state, new Move(0, 2));   // carrot square
        play(fm, state, new Move(0, 4));   // p1, "3" square
        play(fm, state, new Move(0, 7));   // p2, lettuce square
        assertEquals(before, hareTypes(state));
        assertEquals(12, state.getNUnseenHareCards());
    }

    // ---------------------------------------------------------------- your last turn costs nothing

    @Test
    public void lastTurnFreeRefundsTheCostOfTheMove() {
        stackHareDeck(state, LAST_TURN_FREE);
        place(state, 0, 2, 40, 3);
        play(fm, state, new Move(2, 6));
        assertEquals(40 - 10 + 10, state.getCarrots(0));   // 4 squares cost 1+2+3+4 = 10, refunded
        assertEquals(6, state.getSquare(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    // ---------------------------------------------------------------- draw or discard 10 carrots

    @Test
    public void drawOrDiscardLetsTheSamePlayerChooseAndDiscardingPays10() {
        stackHareDeck(state, DRAW_OR_DISCARD);
        play(fm, state, new Move(0, 1));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(65 - 1, state.getCarrots(0));
        assertEquals(Set.<AbstractAction>of(new ChewCarrot(true), new ChewCarrot(false)), actions(fm, state));
        List<HareCard.Type> afterDraw = hareTypes(state);
        assertEquals(DRAW_OR_DISCARD, afterDraw.get(11));
        assertEquals(11, state.getNUnseenHareCards());

        play(fm, state, new ChewCarrot(false));
        assertEquals(64 - 10, state.getCarrots(0));
        assertEquals(1, state.getSquare(0));
        assertEquals(1, state.getCurrentPlayer());
        // the choice draws no further card
        assertEquals(afterDraw, hareTypes(state));
        assertEquals(11, state.getNUnseenHareCards());
    }

    @Test
    public void drawOrDiscardDrawing10AlsoPassesTheTurn() {
        stackHareDeck(state, DRAW_OR_DISCARD);
        play(fm, state, new Move(0, 1));
        play(fm, state, new ChewCarrot(true));
        assertEquals(64 + 10, state.getCarrots(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void drawOrDiscardWithFewerThan10CarrotsOffersOnlyTheDraw() {
        stackHareDeck(state, DRAW_OR_DISCARD);
        place(state, 0, 0, 5, 3);
        play(fm, state, new Move(0, 1));
        assertEquals(5 - 1, state.getCarrots(0));
        assertEquals(Set.<AbstractAction>of(new ChewCarrot(true)), actions(fm, state));
        play(fm, state, new ChewCarrot(true));
        assertEquals(4 + 10, state.getCarrots(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void drawOrDiscardByTheLastPlayerEndsTheRoundOnlyAfterTheChoice() {
        stackHareDeck(state, DRAW_OR_DISCARD);
        state.setTurnOwner(3);
        play(fm, state, new Move(0, 1));
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(0, state.getRoundCounter());
        play(fm, state, new ChewCarrot(true));
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(64 + 10, state.getCarrots(3));
    }

    // ---------------------------------------------------------------- leap ahead one position

    @Test
    public void leapAheadGoesToTheFirstFreeNonTortoiseSquareBeyondTheRunnerAhead() {
        stackHareDeck(state, LEAP_AHEAD_ONE_POSITION);
        place(state, 1, 9, 65, 3);
        place(state, 2, 10, 65, 3);
        play(fm, state, new Move(0, 3));
        // runner ahead on 9; 10 occupied, 11 tortoise, 12 free
        assertEquals(12, state.getSquare(0));
        assertEquals(65 - 6, state.getCarrots(0));   // 1+2+3; the card move is free
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void leapAheadSkipsALettuceSquareWithoutALettuce() {
        stackHareDeck(state, LEAP_AHEAD_ONE_POSITION);
        place(state, 0, 13, 30, 0);
        place(state, 1, 21, 65, 3);
        play(fm, state, new Move(13, 14));
        assertEquals(23, state.getSquare(0));        // 22 is lettuce, no lettuce held
        assertEquals(30 - 1, state.getCarrots(0));
        assertFalse(state.hasLettuceToChew(0));
    }

    @Test
    public void leapAheadOntoALettuceSquareWithALettuceMeansChewingNextTurn() {
        stackHareDeck(state, LEAP_AHEAD_ONE_POSITION);
        place(state, 0, 13, 30, 2);
        place(state, 1, 21, 65, 3);
        play(fm, state, new Move(13, 14));
        assertEquals(22, state.getSquare(0));
        assertEquals(2, state.getLettuces(0));
        assertTrue(state.hasLettuceToChew(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void leapAheadHasNoEffectWhenOnlyRunnersHomeAreAhead() {
        stackHareDeck(state, LEAP_AHEAD_ONE_POSITION);
        putHome(state, 3, 1);
        place(state, 0, 59, 30, 0);
        play(fm, state, new Move(59, 61));
        assertEquals(61, state.getSquare(0));
        assertEquals(30 - 3, state.getCarrots(0));   // 1+2
        assertEquals(0, state.getFinishPosition(0));
        assertEquals("the card was drawn", 11, state.getNUnseenHareCards());
    }

    @Test
    public void leapAheadHasNoEffectWithNoSquareBeyondTheRunnerAhead() {
        stackHareDeck(state, LEAP_AHEAD_ONE_POSITION);
        place(state, 0, 59, 30, 0);
        place(state, 1, 63, 30, 0);
        play(fm, state, new Move(59, 61));
        // runner ahead on 63; nothing beyond it but HOME, which a card never reaches
        assertEquals(61, state.getSquare(0));
        assertEquals(0, state.getFinishPosition(0));
        assertEquals("the card was drawn", 11, state.getNUnseenHareCards());
    }

    @Test
    public void aCardMoveOntoAHareSquareDrawsNoFurtherCard() {
        stackHareDeck(state, LEAP_AHEAD_ONE_POSITION, DRAW_OR_DISCARD);
        List<HareCard.Type> before = hareTypes(state);
        place(state, 0, 13, 30, 3);
        place(state, 1, 24, 30, 3);
        play(fm, state, new Move(13, 14));
        assertEquals(25, state.getSquare(0));        // hare square beyond the runner on 24
        assertEquals(1, state.getCurrentPlayer());   // no DRAW_OR_DISCARD choice
        assertEquals(topToBottom(before, 1), hareTypes(state));
        assertEquals(11, state.getNUnseenHareCards());
        assertEquals(30 - 1, state.getCarrots(0));
    }

    // ---------------------------------------------------------------- next / previous carrot square

    @Test
    public void nextCarrotSquareGoesToTheNextUnoccupiedCarrotSquare() {
        stackHareDeck(state, NEXT_CARROT_SQUARE);
        place(state, 1, 5, 65, 3);
        play(fm, state, new Move(0, 3));
        assertEquals(13, state.getSquare(0));        // carrot 5 occupied, next 13
        assertEquals(65 - 6, state.getCarrots(0));
    }

    @Test
    public void nextCarrotSquareHasNoEffectWithNoneAhead() {
        stackHareDeck(state, NEXT_CARROT_SQUARE);
        place(state, 0, 59, 30, 0);
        play(fm, state, new Move(59, 61));
        assertEquals(61, state.getSquare(0));        // last carrot square is 59
        assertEquals(30 - 3, state.getCarrots(0));
        assertEquals("the card was drawn", 11, state.getNUnseenHareCards());
    }

    @Test
    public void previousCarrotSquareGoesBackToThePreviousUnoccupiedCarrotSquare() {
        stackHareDeck(state, PREVIOUS_CARROT_SQUARE);
        place(state, 1, 5, 65, 3);
        play(fm, state, new Move(0, 6));
        assertEquals(2, state.getSquare(0));         // carrot 5 occupied, previous 2
        assertEquals(65 - 21, state.getCarrots(0));  // 1+2+...+6; the card move is free
    }

    @Test
    public void previousCarrotSquareHasNoEffectWhenAllBehindAreOccupied() {
        stackHareDeck(state, PREVIOUS_CARROT_SQUARE);
        place(state, 1, 5, 65, 3);
        place(state, 2, 2, 65, 3);
        play(fm, state, new Move(0, 6));
        assertEquals(6, state.getSquare(0));
        assertEquals("the card was drawn", 11, state.getNUnseenHareCards());
    }

    // ---------------------------------------------------------------- fall back one position

    @Test
    public void fallBackGoesBelowTheRunnerBehindAndMayLandOnATortoise() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION);
        place(state, 0, 9, 30, 3);
        place(state, 1, 12, 30, 3);
        play(fm, state, new Move(9, 14));
        assertEquals(11, state.getSquare(0));        // runner behind on 12; 11 is a free tortoise square
        assertEquals(30 - 15, state.getCarrots(0));  // 1+2+3+4+5; the card move is free
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void fallBackSkipsOccupiedSquares() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION);
        place(state, 0, 9, 30, 3);
        place(state, 1, 12, 30, 3);
        place(state, 2, 11, 30, 3);
        play(fm, state, new Move(9, 14));
        assertEquals(10, state.getSquare(0));        // runner behind on 12 (the highest below 14); 11 occupied
    }

    @Test
    public void fallBackSkipsALettuceSquareWithoutALettuce() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION);
        place(state, 0, 21, 30, 0);
        place(state, 1, 23, 30, 3);
        play(fm, state, new Move(21, 25));
        assertEquals(21, state.getSquare(0));        // runner behind on 23; 22 lettuce skipped; 21 now free
        assertEquals(30 - 10, state.getCarrots(0));  // 1+2+3+4
        assertFalse(state.hasLettuceToChew(0));
    }

    @Test
    public void fallBackOntoALettuceSquareWithALettuceMeansChewingNextTurn() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION);
        place(state, 0, 21, 30, 1);
        place(state, 1, 23, 30, 3);
        play(fm, state, new Move(21, 25));
        assertEquals(22, state.getSquare(0));
        assertTrue(state.hasLettuceToChew(0));
        assertEquals(1, state.getLettuces(0));
    }

    @Test
    public void fallBackHasNoEffectWhenEveryoneElseIsAtStart() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION);
        play(fm, state, new Move(0, 1));
        assertEquals(1, state.getSquare(0));
        assertEquals("the card was drawn", 11, state.getNUnseenHareCards());
    }

    @Test
    public void fallBackHasNoEffectWithNoFreeSquareBelowTheRunnerBehind() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION);
        place(state, 1, 1, 65, 3);
        play(fm, state, new Move(0, 3));
        assertEquals(3, state.getSquare(0));         // runner behind on 1: nothing below it but START
        assertEquals("the card was drawn", 11, state.getNUnseenHareCards());
    }

    @Test
    public void fallBackOntoAHareSquareDrawsNoFurtherCard() {
        stackHareDeck(state, FALL_BACK_ONE_POSITION, DRAW_OR_DISCARD);
        place(state, 1, 4, 65, 3);
        play(fm, state, new Move(0, 6));
        assertEquals(3, state.getSquare(0));         // runner behind on 4; 3 is a free hare square
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(11, state.getNUnseenHareCards());
        assertEquals(65 - 21, state.getCarrots(0));
    }

    // ---------------------------------------------------------------- have another turn

    @Test
    public void anotherTurnKeepsTheTurnWithTheSamePlayer() {
        stackHareDeck(state, ANOTHER_TURN);
        play(fm, state, new Move(0, 1));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getRoundCounter());
        assertTrue(actions(fm, state).contains(new Move(1, 2)));
        play(fm, state, new Move(1, 2));
        assertEquals(65 - 1 - 1, state.getCarrots(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getSquare(1));
    }

    @Test
    public void anotherTurnForTheLastPlayerDoesNotEndTheRound() {
        stackHareDeck(state, ANOTHER_TURN);
        state.setTurnOwner(3);
        play(fm, state, new Move(0, 1));
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void anotherTurnStartsWithTheStuckCheck() {
        stackHareDeck(state, ANOTHER_TURN);
        place(state, 0, 0, 1, 3);
        play(fm, state, new Move(0, 1));
        // 0 carrots on hare square 1: no affordable move, no tortoise behind -> back to START with 65
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getSquare(0));
        assertEquals(65, state.getCarrots(0));
    }

    // ---------------------------------------------------------------- miss a turn

    @Test
    public void missATurnSkipsThePlayerNextTimeRoundAndCountsTheRoundWrap() {
        stackHareDeck(state, MISS_A_TURN);
        play(fm, state, new Move(0, 1));
        assertTrue(state.missesNextTurn(0));
        assertEquals(1, state.getCurrentPlayer());
        play(fm, state, new Move(0, 2));   // p1
        play(fm, state, new Move(0, 4));   // p2
        play(fm, state, new Move(0, 5));   // p3: p0 is skipped
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.missesNextTurn(0));
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getSquare(0));
        assertEquals(64, state.getCarrots(0));
    }

    @Test
    public void aSkippedPlayerGetsNoPayoutButTheNextPlayerDoes() {
        place(state, 0, 9, 20, 3);    // "4" square, 4th
        state.missNextTurn[0] = true;
        place(state, 1, 12, 20, 3);   // "3" square, 3rd
        place(state, 2, 30, 20, 3);
        place(state, 3, 5, 65, 3);
        state.setTurnOwner(3);

        play(fm, state, new Move(5, 13));   // p3: 1+2+...+8 = 36; order now p2 30, p3 13, p1 12, p0 9
        assertEquals(65 - 36, state.getCarrots(3));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(20, state.getCarrots(0));          // no 10 x 4th for the skipped player
        assertFalse(state.missesNextTurn(0));
        assertEquals(20 + 30, state.getCarrots(1));     // 10 x 3rd
        assertEquals(1, state.getRoundCounter());
    }

    @Test
    public void aSkippedPlayerIsNotCheckedForBeingStuckUntilTheirNextTurn() {
        place(state, 0, 6, 0, 2);     // stuck: hare square, no carrots, no tortoise behind
        state.missNextTurn[0] = true;
        state.setTurnOwner(3);

        play(fm, state, new Move(0, 2));    // p3
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(6, state.getSquare(0));
        assertEquals(0, state.getCarrots(0));
        play(fm, state, new Move(0, 4));    // p1
        play(fm, state, new Move(0, 5));    // p2
        play(fm, state, new Move(2, 7));    // p3: lettuce square, 1+2+3+4+5 = 15
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getSquare(0));
        assertEquals(65, state.getCarrots(0));
    }

    // ---------------------------------------------------------------- chew a lettuce

    @Test
    public void chewALettuceDiscardsOneAndDrawsForRacePosition() {
        stackHareDeck(state, CHEW_A_LETTUCE);
        place(state, 1, 10, 65, 3);
        place(state, 2, 12, 65, 3);
        play(fm, state, new Move(0, 6));
        // 65 - (1+...+6) + 10 x 3rd (behind 12 and 10)
        assertEquals(65 - 21 + 30, state.getCarrots(0));
        assertEquals(3 - 1, state.getLettuces(0));
        assertFalse(state.hasLettuceToChew(0));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void chewALettuceWithoutALettuceHasNoEffect() {
        stackHareDeck(state, CHEW_A_LETTUCE);
        place(state, 0, 0, 65, 0);
        play(fm, state, new Move(0, 6));
        assertEquals(65 - 21, state.getCarrots(0));
        assertEquals(0, state.getLettuces(0));
        assertEquals("the card was drawn", 11, state.getNUnseenHareCards());
    }
}
