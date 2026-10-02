package games.rummy;

import core.components.FrenchCard;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import org.junit.Test;

import java.util.List;

import static core.CoreConstants.GameResult.*;
import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The multi-deal game (RummyParameters.targetScore > 0), tested by calling RummyForwardModel.endDeal directly on
 * arranged hands: who scores what at the end of a deal, the end of the game at the target (and at the deal limit),
 * and the next deal (a fresh shuffle and deal, the first player moving left one each deal). With targetScore 0 the
 * game still ends after the first deal.
 */
public class RummyDealScoringTest {

    private final RummyForwardModel fm = new RummyForwardModel();

    private RummyGameState newMultiDealState(int nPlayers, int targetScore, long seed) {
        RummyParameters params = RummyTestUtils.valetParams();
        params.setParameterValue("targetScore", targetScore);
        return newState(params, nPlayers, seed, fm);
    }

    /** Asserts the state is the start of a new deal with `first` to play. */
    private void assertNewDeal(RummyGameState state, int deal, int first) {
        int n = state.getNPlayers();
        int handSize = ((RummyParameters) state.getGameParameters()).handSize(n);
        assertTrue("the game goes on", state.isNotTerminal());
        assertEquals("round counter (deals completed)", deal, state.getRoundCounter());
        assertEquals("first player of deal " + deal, first, state.getFirstPlayer());
        assertEquals("player to act", first, state.getCurrentPlayer());
        assertEquals("turn counter reset", 0, state.getTurnCounter());
        for (int p = 0; p < n; p++)
            assertEquals("hand of " + p, handSize, state.getPlayerHand(p).getSize());
        assertEquals(1, state.getDiscardPile().getSize());
        assertEquals(52 - n * handSize - 1, state.getDrawDeck().getSize());
        assertTrue("melds cleared", state.getMelds().isEmpty());
        assertNull(state.getTakenCard());
        assertFalse(state.hasMeldedThisTurn());
        assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
        assertAllCardsPresent(state);
    }

    @Test
    public void thePlayerWhoWentOutScoresThePointsInTheOtherHandsAndANewDealStarts() {
        RummyGameState state = newMultiDealState(3, 100, 31);
        state.playerScores = new int[]{10, 5, 0};
        addMeld(state, "4H", "5H", "6H");
        giveHand(state, 0);
        giveHand(state, 1, "KS", "5D", "AH");
        giveHand(state, 2, "QC", "2H");

        fm.endDeal(state);

        // player 1: K 10 + 5 + A 1 = 16; player 2: Q 10 + 2 = 12; player 0 scores 16 + 12 = 28, 10 + 28 = 38
        assertArrayEquals(new int[]{38, 5, 0}, state.playerScores);
        assertEquals(38, state.getPlayerScore(0));
        // with a target score, the game score is the running total
        assertEquals(38.0, state.getGameScore(0), 0.0);
        assertEquals(5.0, state.getGameScore(1), 0.0);
        assertEquals(0.0, state.getGameScore(2), 0.0);
        // 38 < 100: the second deal (deal 1) starts with player 1 % 3 = 1
        assertNewDeal(state, 1, 1);
    }

    @Test
    public void withNoEmptyHandTheSingleFewestPointsScoresTheOtherHandsButNotItsOwn() {
        RummyGameState state = newMultiDealState(2, 100, 32);
        giveHand(state, 0, "KS", "QS");
        giveHand(state, 1, "3D", "4C");

        fm.endDeal(state);

        // player 0: 10 + 10 = 20; player 1: 3 + 4 = 7, the fewest: scores player 0's 20 (not 20 + 7 = 27)
        assertArrayEquals(new int[]{0, 20}, state.playerScores);
        assertNewDeal(state, 1, 1);
    }

    @Test
    public void nobodyScoresWhenTheFewestPointsAreTied() {
        RummyGameState state = newMultiDealState(3, 100, 33);
        state.playerScores = new int[]{4, 8, 12};
        giveHand(state, 0, "3D", "4C");     // 7
        giveHand(state, 1, "7S");           // 7
        giveHand(state, 2, "KS", "QD");     // 20

        fm.endDeal(state);

        // players 0 and 1 tie on 7: no winner, the totals are unchanged, and the next deal starts
        assertArrayEquals(new int[]{4, 8, 12}, state.playerScores);
        assertNewDeal(state, 1, 1);
    }

    @Test
    public void theGameEndsWhenATotalReachesTheTargetExactly() {
        RummyGameState state = newMultiDealState(2, 50, 34);
        state.playerScores = new int[]{35, 30};
        giveHand(state, 0);
        giveHand(state, 1, "KS", "5D");

        fm.endDeal(state);

        // player 0 scores K 10 + 5 = 15: 35 + 15 = 50 >= 50, the game ends
        assertArrayEquals(new int[]{50, 30}, state.playerScores);
        assertFalse(state.isNotTerminal());
        assertEquals(50.0, state.getGameScore(0), 0.0);
        assertEquals(30.0, state.getGameScore(1), 0.0);
        assertArrayEquals(new core.CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        // no new deal: the hands are as they were at the end of the deal
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(setOf(state.getPlayerHand(1)), java.util.Set.copyOf(cards("KS", "5D")));
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void theResultsRankThePlayersByTheirTotals() {
        RummyGameState state = newMultiDealState(3, 50, 35);
        state.playerScores = new int[]{20, 45, 20};
        giveHand(state, 0, "2C", "3C", "9H");   // 14
        giveHand(state, 1, "AS");               // 1, the fewest
        giveHand(state, 2, "JD", "10S");        // 20

        fm.endDeal(state);

        // player 1 scores 14 + 20 = 34: 45 + 34 = 79 >= 50; players 0 and 2 share second place on 20
        assertArrayEquals(new int[]{20, 79, 20}, state.playerScores);
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new core.CoreConstants.GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals(1, state.getOrdinalPosition(1));
        assertEquals(2, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(2));
    }

    @Test
    public void theFirstPlayerMovesLeftOneEachDealAndTheTurnCounterIsReset() {
        RummyGameState state = newMultiDealState(3, 100, 36);
        for (int deal = 1; deal <= 4; deal++) {
            // one turn of the deal, so the turn counter is not 0 at its end
            FrenchCard top = state.getDrawDeck().peek();
            fm.next(state, new DrawCard(false));
            fm.next(state, new Discard(top));
            assertEquals(1, state.getTurnCounter());
            // a tie for the fewest: nobody scores, so the game goes on
            giveHand(state, 0, "5S");
            giveHand(state, 1, "5D");
            giveHand(state, 2, "5C");
            fm.endDeal(state);
            // deal 1 starts with player 1, deal 2 with 2, deal 3 with 3 % 3 = 0, deal 4 with 1
            assertNewDeal(state, deal, deal % 3);
        }
        assertArrayEquals(new int[]{0, 0, 0}, state.playerScores);
    }

    @Test
    public void theNewDealIsAFreshShuffleWithTheGameRandomGenerator() {
        // two states arranged alike, differing only in the random generator's position before the deal ends
        RummyGameState a = newMultiDealState(2, 100, 37);
        RummyGameState b = newMultiDealState(2, 100, 37);
        RummyGameState c = newMultiDealState(2, 100, 37);
        for (RummyGameState s : List.of(a, b, c)) {
            giveHand(s, 0, "5S");
            giveHand(s, 1, "5D");
        }
        c.getRnd().nextInt();
        fm.endDeal(a);
        fm.endDeal(b);
        fm.endDeal(c);
        assertEquals("same generator, same deal", a.getPlayerHand(0).getComponents(), b.getPlayerHand(0).getComponents());
        assertEquals(a.getDrawDeck().getComponents(), b.getDrawDeck().getComponents());
        assertNotEquals("the deal follows the generator", a.getDrawDeck().getComponents(), c.getDrawDeck().getComponents());
        assertNewDeal(a, 1, 1);
    }

    @Test
    public void theGameEndsAfterTheHundredthDealEvenBelowTheTarget() {
        RummyGameState state = newMultiDealState(2, 100, 38);
        state.playerScores = new int[]{20, 10};
        for (int deal = 1; deal <= 100; deal++) {
            // tied hands: nobody scores
            giveHand(state, 0, "KS");
            giveHand(state, 1, "KD");
            fm.endDeal(state);
            if (deal < 100)
                assertTrue("still playing after deal " + deal, state.isNotTerminal());
        }
        // RummyParameters sets maxRounds to 100, so the framework ends the game at the end of the 100th deal,
        // ranked by the totals
        assertFalse(state.isNotTerminal());
        assertEquals(100, state.getRoundCounter());
        assertEquals(20.0, state.getGameScore(0), 0.0);
        assertEquals(10.0, state.getGameScore(1), 0.0);
        assertArrayEquals(new core.CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    // --- a pair differing only by targetScore: the same deal end with target 0 (one deal) and target 50 ---

    private RummyGameState endOfDealWithTarget(int targetScore) {
        RummyGameState state = newMultiDealState(2, targetScore, 39);
        giveHand(state, 0);
        giveHand(state, 1, "KS", "5D");
        fm.endDeal(state);
        return state;
    }

    @Test
    public void withTargetScoreZeroTheGameEndsAfterTheFirstDealScoredByHandPoints() {
        RummyGameState state = endOfDealWithTarget(0);
        assertFalse(state.isNotTerminal());
        // a single deal: the score is minus the points in hand (K 10 + 5 = 15), and no totals are kept
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(-15.0, state.getGameScore(1), 0.0);
        assertArrayEquals(new int[]{0, 0}, state.playerScores);
        assertArrayEquals(new core.CoreConstants.GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void withTargetScoreFiftyTheSameDealEndScoresAndDealsAgain() {
        RummyGameState state = endOfDealWithTarget(50);
        // player 0 went out and scores K 10 + 5 = 15 < 50
        assertArrayEquals(new int[]{15, 0}, state.playerScores);
        assertEquals(15.0, state.getGameScore(0), 0.0);
        assertNewDeal(state, 1, 1);
    }
}
