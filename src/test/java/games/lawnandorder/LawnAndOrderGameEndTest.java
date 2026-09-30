package games.lawnandorder;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static core.CoreConstants.GameResult.*;
import static games.lawnandorder.LawnAndOrderGameState.Phase.CONTINUE_OR_PASS;
import static games.lawnandorder.LawnAndOrderRoundTest.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of the game and its results, by qualifying on all three tracks and by reaching maxRounds.
 * <p>
 * Each test sets the tracks before a round in which player 0 scores the rulebook example lawn (4/2/1), player 1 three
 * Red Water Features (2/2/0) and player 2 (if any) nothing, and everyone passes.
 */
public class LawnAndOrderGameEndTest {

    LawnAndOrderForwardModel fm;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
    }

    private LawnAndOrderGameState scoreRound(int[]... tracksBefore) {
        LawnAndOrderGameState state = newState(tracksBefore.length, 2);
        for (int p = 0; p < tracksBefore.length; p++)
            state.trackScores[p] = tracksBefore[p].clone();
        setLawn(state, 0, EXAMPLE_LAWN);
        setLawn(state, 1, RED_WATER_LAWN);
        state.setGamePhase(CONTINUE_OR_PASS);
        everyonePasses(fm, state);
        return state;
    }

    @Test
    public void tenOnAllThreeTracksWinsEvenAgainstAHigherTotal() {
        LawnAndOrderGameState state = scoreRound(new int[]{9, 9, 9}, new int[]{20, 20, 0});
        // player 0: 13/11/10 qualifies (total 34); player 1: 22/22/0 does not, though their total is 44
        assertArrayEquals(new int[]{13, 11, 10}, tracks(state, 0));
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertArrayEquals(new Object[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void tenOnTwoTracksIsNotEnoughAndANewRoundStarts() {
        LawnAndOrderGameState state = scoreRound(new int[]{9, 9, 8}, new int[]{0, 0, 0});
        // player 0: 13/11/9
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertArrayEquals(new int[]{13, 11, 9}, tracks(state, 0));
        assertArrayEquals(new int[]{2, 2, 0}, tracks(state, 1));
    }

    @Test
    public void ofTwoQualifiersTheHigherTotalWins() {
        LawnAndOrderGameState state = scoreRound(new int[]{9, 9, 9}, new int[]{8, 8, 10}, new int[]{30, 30, 9});
        // player 0: 13/11/10 = 34; player 1: 10/10/10 = 30; player 2 has 69 but only 9 on Feature
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new Object[]{WIN_GAME, LOSE_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void qualifiersTiedOnTheHighestTotalDraw() {
        LawnAndOrderGameState state = scoreRound(new int[]{9, 9, 9}, new int[]{10, 10, 10}, new int[]{30, 30, 9});
        // player 0: 13/11/10 = 34; player 1: 12/12/10 = 34; player 2 does not qualify
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new Object[]{DRAW_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    /** A state with maxRounds set before setup, and the tracks set. */
    private static LawnAndOrderGameState withMaxRounds(int maxRounds, int[]... tracksBefore) {
        LawnAndOrderGameState state = newState(tracksBefore.length, 2, Map.of("maxRounds", maxRounds));
        for (int p = 0; p < tracksBefore.length; p++)
            state.trackScores[p] = tracksBefore[p].clone();
        return state;
    }

    /** Player 0 scores the example lawn (4/2/1), player 1 three Red Water Features (2/2/0), and everyone passes. */
    private void passWithTheExampleLawns(LawnAndOrderGameState state) {
        setLawn(state, 0, EXAMPLE_LAWN);
        setLawn(state, 1, RED_WATER_LAWN);
        state.setGamePhase(CONTINUE_OR_PASS);
        everyonePasses(fm, state);
    }

    @Test
    public void afterMaxRoundsTheGameEndsAndTheHighestTotalWins() {
        LawnAndOrderGameState state = withMaxRounds(2, new int[]{0, 0, 0}, new int[]{5, 5, 0});
        passWithTheExampleLawns(state);
        // round 1 of 2 completed: player 0 4/2/1, player 1 7/7/0 - nobody qualifies, the game goes on
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertArrayEquals(new int[]{4, 2, 1}, tracks(state, 0));
        assertArrayEquals(new int[]{7, 7, 0}, tracks(state, 1));

        passWithTheExampleLawns(state);
        // round 2 of 2: player 0 8/4/2 = 14, player 1 9/9/0 = 18; nobody qualifies -> the highest total wins
        assertArrayEquals(new int[]{8, 4, 2}, tracks(state, 0));
        assertArrayEquals(new int[]{9, 9, 0}, tracks(state, 1));
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertEquals(2, state.getRoundCounter());
        assertArrayEquals(new Object[]{LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void aCopyTakenBeforeTheLastRoundAlsoEndsAtMaxRounds() {
        LawnAndOrderGameState state = withMaxRounds(2, new int[]{0, 0, 0}, new int[]{5, 5, 0});
        passWithTheExampleLawns(state);
        LawnAndOrderGameState copy = (LawnAndOrderGameState) state.copy();
        passWithTheExampleLawns(copy);
        // as above: 14 against 18 after the second round
        assertFalse(copy.isNotTerminal());
        assertArrayEquals(new Object[]{LOSE_GAME, WIN_GAME}, copy.getPlayerResults());
    }

    @Test
    public void playersTiedOnTheHighestTotalAtMaxRoundsDraw() {
        LawnAndOrderGameState state = withMaxRounds(2, new int[]{0, 0, 0}, new int[]{3, 3, 0}, new int[]{0, 0, 0});
        passWithTheExampleLawns(state);
        passWithTheExampleLawns(state);
        // player 0: 2 x 4/2/1 = 8/4/2 = 14; player 1: 3+2+2 / 3+2+2 / 0 = 7/7/0 = 14; player 2: no lawn, 0
        assertArrayEquals(new int[]{7, 7, 0}, tracks(state, 1));
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertArrayEquals(new Object[]{DRAW_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void aQualifierInTheLastRoundStillWinsByTheQualifierRule() {
        LawnAndOrderGameState state = withMaxRounds(1, new int[]{9, 9, 9}, new int[]{20, 20, 0});
        passWithTheExampleLawns(state);
        // player 0: 13/11/10 = 34 qualifies; player 1: 22/22/0 = 44 does not -> player 0 wins despite the lower total
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertArrayEquals(new Object[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void byDefaultTheGameEndsAfterThirtyRounds() {
        LawnAndOrderGameState state = newState(2, 2);
        // every round: nobody plays, everyone passes with an empty lawn and scores 0
        for (int round = 1; round <= 29; round++) {
            state.setGamePhase(CONTINUE_OR_PASS);
            everyonePasses(fm, state);
        }
        assertTrue(state.isNotTerminal());
        assertEquals(29, state.getRoundCounter());
        state.setGamePhase(CONTINUE_OR_PASS);
        everyonePasses(fm, state);
        // round 30 completed: 0 against 0 -> both on the highest total -> draw
        assertFalse(state.isNotTerminal());
        assertEquals(GAME_END, state.getGameStatus());
        assertArrayEquals(new Object[]{DRAW_GAME, DRAW_GAME}, state.getPlayerResults());
    }
}
