package games.skitgubbe;

import core.CoreConstants;
import games.skitgubbe.actions.PlayCard;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.skitgubbe.SkitgubbeTestUtils.*;
import static org.junit.Assert.*;

/**
 * exitActions (the phase-two action count when each player went out; 0 for players out when phase two begins) and
 * SkitgubbeParameters.exitOrderTiebreak: players on equal exitScores rank by who went out first when true, and
 * share the place when false (the default). Trumps Hearts unless stated.
 */
public class SkitgubbeExitOrderTest {

    SkitgubbeForwardModel fm = new SkitgubbeForwardModel();

    private static SkitgubbeParameters params(boolean exitOrderTiebreak) {
        SkitgubbeParameters params = new SkitgubbeParameters();
        params.setParameterValue("exitOrderTiebreak", exitOrderTiebreak);
        return params;
    }

    private void play(SkitgubbeGameState state, String code) {
        fm.next(state, new PlayCard(card(code)));
    }

    private static void assertOrdinals(SkitgubbeGameState state, int... expected) {
        for (int p = 0; p < expected.length; p++)
            assertEquals("ordinal of " + p, expected[p], state.getOrdinalPosition(p));
    }

    // ---------------- exitActions ----------------

    @Test
    public void exitActionsRecordThePhaseTwoActionOnWhichEachPlayerWentOut() {
        SkitgubbeGameState state = newState(3, 42, params(false));
        // A keeps 3D to the end, so A is still holding when C goes out
        arrangePhaseTwo(state, "H", 0, "5S 2C 3D", "9S", "JS 4C");
        play(state, "5S");     // action 1: A leads
        play(state, "9S");     // action 2: B beats with his last card -> out
        assertEquals(2, state.getExitActions(1));
        assertEquals(0, state.getExitActions(0));
        assertEquals(0, state.getExitActions(2));
        play(state, "JS");     // action 3: C completes the trick of 3
        play(state, "2C");     // action 4: A leads (trickSize 2: A, C)
        play(state, "4C");     // action 5: C beats (4C > 2C) with his last card -> out; only A holds: game over

        assertFalse(state.isNotTerminal());
        assertEquals(5, state.phaseTwoActions);
        assertEquals(0, state.getExitActions(0));    // still holding: 0
        assertEquals(2, state.getExitActions(1));
        assertEquals(5, state.getExitActions(2));
    }

    /** Phase one ends with player 2 already out; phase two is then played until player 0 goes out too. */
    private SkitgubbeGameState outAtTheStartThenOutInPhaseTwo(boolean exitOrderTiebreak) {
        SkitgubbeGameState state = newState(3, 42, params(exitOrderTiebreak));
        moveTo(state, state.heldCards.get(2), "QC");  // player 2's only card, held from a bounce
        giveTrumpCard(state, 1, "JD");
        giveHand(state, 0, "KS");
        giveHand(state, 1, "5H", "8C");
        giveHand(state, 2);
        leaveDrawDeck(state, 1);                      // the other 47 cards are player 1's

        play(state, "KS");
        play(state, "5H");     // KS wins; player 0 should lead with no cards: phase two begins
        assertEquals(SkitgubbeGameState.Phase.PHASE_TWO, state.getGamePhase());
        // player 0 {KS 5H QC}; player 1 the other 49; player 2 out with exitScore = trickSize 2; trumps Diamonds
        assertEquals(2, state.getExitScore(2));
        assertEquals(0, state.getExitActions(2));    // out when phase two began
        assertEquals(1, state.getCurrentPlayer());   // the trump player leads

        play(state, "2S");     // action 1: player 1 leads
        play(state, "KS");     // action 2: player 0 beats; 2 cards = trickSize 2, complete
        play(state, "2H");     // action 3: player 1 leads (the next holder after player 0)
        play(state, "5H");     // action 4: player 0 beats, complete
        play(state, "2C");     // action 5
        play(state, "QC");     // action 6: player 0's last card -> out with exitScore = trickSize 2; game over

        assertFalse(state.isNotTerminal());
        assertEquals(6, state.getExitActions(0));
        assertEquals(0, state.getExitActions(1));    // still holding
        assertEquals(0, state.getExitActions(2));
        assertEquals(2.0, state.getGameScore(0), 0.0);
        assertEquals(0.0, state.getGameScore(1), 0.0);
        assertEquals(2.0, state.getGameScore(2), 0.0);
        assertAllCardsPresent(state);
        return state;
    }

    @Test
    public void withExitOrderTiebreakAPlayerOutWhenPhaseTwoBeganRanksAboveOneOutLaterOnTheSameScore() {
        SkitgubbeGameState state = outAtTheStartThenOutInPhaseTwo(true);
        // equal scores 2: player 2 out at 0 actions ranks above player 0 out at 6
        assertOrdinals(state, 2, 3, 1);
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void withoutExitOrderTiebreakAPlayerOutWhenPhaseTwoBeganSharesThePlaceWithOneOutLaterOnTheSameScore() {
        SkitgubbeGameState state = outAtTheStartThenOutInPhaseTwo(false);
        // players 0 and 2 share first on 2; two players at ordinal 1 is a draw in the default endGame
        assertOrdinals(state, 1, 3, 1);
        assertArrayEquals(new CoreConstants.GameResult[]{DRAW_GAME, LOSE_GAME, DRAW_GAME}, state.getPlayerResults());
    }

    // ---------------- two players out in the same trick ----------------

    /** 3 players: A leads his last card (exitScore 3), B beats with his last card (3): only C holds, game over. */
    private SkitgubbeGameState doubleExit(boolean exitOrderTiebreak) {
        SkitgubbeGameState state = newState(3, 42, params(exitOrderTiebreak));
        arrangePhaseTwo(state, "H", 0, "5S", "9S", "JS 4C");
        play(state, "5S");     // action 1: A out, exitScore = trickSize 3
        assertTrue(state.isNotTerminal());           // B and C still hold cards
        play(state, "9S");     // action 2: B out, exitScore 3 (trickSize fixed for the trick)

        assertFalse(state.isNotTerminal());
        assertEquals(3.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);
        assertEquals(1, state.getExitActions(0));
        assertEquals(2, state.getExitActions(1));
        assertEquals(0, state.getExitActions(2));
        return state;
    }

    @Test
    public void withExitOrderTiebreakTheFirstOfTwoPlayersOutInTheSameTrickWins() {
        SkitgubbeGameState state = doubleExit(true);
        // A (out at 1) before B (out at 2)
        assertOrdinals(state, 1, 2, 3);
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void withoutExitOrderTiebreakTwoPlayersOutInTheSameTrickShareFirstAsADraw() {
        SkitgubbeGameState state = doubleExit(false);
        assertOrdinals(state, 1, 1, 3);
        assertArrayEquals(new CoreConstants.GameResult[]{DRAW_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void withExitOrderTiebreakPlayersStillHoldingCardsStayTied() {
        SkitgubbeParameters params = params(true);
        params.setParameterValue("maxPhaseTwoActions", 4);
        SkitgubbeGameState state = newState(4, 42, params);
        arrangePhaseTwo(state, "H", 0, "5S", "9S", "JS 4C", "QS 3C");
        play(state, "5S");     // action 1: A out, exitScore = trickSize 4
        play(state, "9S");     // action 2: B out, exitScore 4; C and D hold cards
        play(state, "JS");     // action 3
        play(state, "QS");     // action 4: the trick of 4 is complete; 4 actions = the cap: game over

        assertFalse(state.isNotTerminal());
        assertEquals(4.0, state.getGameScore(0), 0.0);
        assertEquals(4.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);   // holds 4C
        assertEquals(0.0, state.getGameScore(3), 0.0);   // holds 3C
        // A (out at 1) before B (out at 2); C and D, both still holding, share third
        assertOrdinals(state, 1, 2, 3, 3);
        assertArrayEquals(new CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME, LOSE_GAME, LOSE_GAME},
                state.getPlayerResults());
    }
}
