package games.cribbage;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import games.cribbage.actions.DiscardToCrib;
import games.cribbage.actions.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.cribbage.CribbageGameState.CribbageGamePhase.Discard;
import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * The target score: as soon as any scoring step (his heels, a card in the play, each part of the show) takes a
 * player to targetScore or more, the game ends and that player wins. targetScore 0 turns the target off.
 * Scores are arranged by writing state.scores directly; the target is set with setParameterValue.
 * Round 1: player 0 deals, player 1 is the non-dealer.
 */
public class CribbageTargetScoreTest {

    CribbageParameters params;
    CribbageGameState state;
    CribbageForwardModel fm;

    @Before
    public void setup() {
        params = new CribbageParameters();
        params.setRandomSeed(42);
        state = new CribbageGameState(params, 2);
        fm = new CribbageForwardModel();
        fm.setup(state);
    }

    private void assertWinner(int winner) {
        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[winner]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1 - winner]);
    }

    // ---------------- targetReached ----------------

    @Test
    public void targetReachedWhenEitherPlayerHasTheTargetOrMore() {
        params.setParameterValue("targetScore", 10);
        state.scores[0] = 9;
        state.scores[1] = 0;
        assertFalse(state.targetReached());
        state.scores[0] = 10;
        assertTrue(state.targetReached());
        state.scores[0] = 0;
        state.scores[1] = 11;
        assertTrue(state.targetReached());
    }

    @Test
    public void theDefaultTargetIs121() {
        state.scores[1] = 120;
        assertFalse(state.targetReached());
        state.scores[1] = 121;
        assertTrue(state.targetReached());
    }

    @Test
    public void targetScoreZeroIsNeverReached() {
        params.setParameterValue("targetScore", 0);
        assertFalse(state.targetReached());
        state.scores[0] = 200;
        state.scores[1] = 150;
        assertFalse(state.targetReached());
    }

    // ---------------- the game ends at the scoring step ----------------

    @Test
    public void hisHeelsReachingTheTargetEndsTheGameBeforeThePlay() {
        params.setParameterValue("targetScore", 10);
        giveHand(state, 1, cards("QC", "AH", "2S", "3S", "4S", "6S"));
        giveHand(state, 0, cards("6D", "2C", "8S", "9S", "10S", "QS"));
        putOnTopOfDrawDeck(state, card("JD"));
        state.scores[0] = 8;
        fm.next(state, new DiscardToCrib(card("QC"), card("AH")));
        fm.next(state, new DiscardToCrib(card("6D"), card("2C")));

        // his heels: 8 + 2 = 10 to the dealer
        assertEquals(10, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertWinner(0);
        // no card was played
        assertEquals(4, state.getPlayerHand(0).getSize());
        assertEquals(4, state.getPlayerHand(1).getSize());
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void aFifteenInThePlayReachingTheTargetEndsTheGameAtThatCard() {
        params.setParameterValue("targetScore", 10);
        arrangePlay(state, cards("8D", "5S"), cards("7S", "4C"));
        state.scores[0] = 9;
        fm.next(state, new PlayCard(card("8D")));   // 8
        assertTrue(state.isNotTerminal());
        fm.next(state, new PlayCard(card("7S")));   // 15: 1 to player 0, 9 + 1 = 10

        // Had the game gone on, 5S (20) and 4C (24, the last card: 1 to player 0) would be played, and the
        // non-dealer's show 8D 5S + KS (K+5 = 15) would give player 1 2 points.
        assertEquals(10, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertWinner(0);
        assertEquals(cards("5S"), state.getPlayerHand(1).getComponents());
        assertEquals(cards("4C"), state.getPlayerHand(0).getComponents());
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void theLastCardPointReachingTheTargetEndsTheGameBeforeTheShow() {
        params.setParameterValue("targetScore", 10);
        arrangePlay(state, cards("JS"), cards("8H"));
        playedEarlier(state, 1, card("5H"), card("5D"), card("5C"));
        playedEarlier(state, 0, card("2H"), card("4H"), card("6H"));
        state.scores[0] = 9;
        fm.next(state, new PlayCard(card("JS")));   // 10
        fm.next(state, new PlayCard(card("8H")));   // 18, the last card: 1 to player 0, 9 + 1 = 10

        // the non-dealer's show (5H 5D 5C JS + KS = 21) is never counted, nor the dealer's flush (4)
        assertEquals(10, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertWinner(0);
        assertEquals(0, state.getRoundCounter());
    }

    /**
     * The only test in which the order of the show shows in the scores: the non-dealer counts first and wins,
     * though the dealer's hand would have taken the dealer past them.
     */
    @Test
    public void theNonDealerReachingTheTargetInTheShowWinsBeforeTheDealerCounts() {
        params.setParameterValue("targetScore", 30);
        arrangePlay(state, cards("JS"), cards("8D"));
        playedEarlier(state, 1, card("5H"), card("5D"), card("5C"));
        playedEarlier(state, 0, card("7C"), card("8C"), card("7D"));
        state.scores[0] = 20;
        state.scores[1] = 10;
        fm.next(state, new PlayCard(card("JS")));   // 10
        fm.next(state, new PlayCard(card("8D")));   // 18, the last card: 1 to player 0 -> 21

        // non-dealer 5H 5D 5C JS + KS = 21 (fifteens 14, pair royal 6, his nobs 1): 10 + 21 = 31 >= 30, wins.
        // dealer 7C 8C 7D 8D + KS would be 12 (7+8 four ways = 8, two pairs = 4): 21 + 12 = 33, but is not counted.
        assertEquals(21, state.getScore(0));
        assertEquals(31, state.getScore(1));
        assertWinner(1);
    }

    @Test
    public void theDealerReachingTheTargetOnTheCribWins() {
        params.setParameterValue("targetScore", 30);
        arrangePlay(state, cards("4H"), cards("6D"));
        replaceCrib(state, cards("5H", "5D", "5C", "JS"));
        state.scores[0] = 20;
        fm.next(state, new PlayCard(card("4H")));   // 4
        fm.next(state, new PlayCard(card("6D")));   // 10, the last card: 1 to player 0 -> 21

        // hands 4H + KS and 6D + KS score 0; crib 5H 5D 5C JS + KS = 21: 21 + 21 = 42 >= 30
        assertEquals(42, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertWinner(0);
        // not dealt again
        assertNotEquals(Discard, state.getGamePhase());
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(0, state.getPlayerHand(1).getSize());
    }

    @Test
    public void targetScoreZeroPlaysAllTheRounds() {
        params.setParameterValue("targetScore", 0);
        state.scores[0] = 130;
        // round 1 (player 0 deals): last card to player 0
        arrangePlay(state, cards("4H"), cards("6D"));
        fm.next(state, new PlayCard(card("4H")));
        fm.next(state, new PlayCard(card("6D")));
        assertTrue(state.isNotTerminal());
        assertEquals(131, state.getScore(0));
        // round 2 (player 1 deals): last card to player 1
        arrangePlay(state, cards("4H"), cards("6D"));
        fm.next(state, new PlayCard(card("4H")));
        fm.next(state, new PlayCard(card("6D")));

        assertEquals(2, state.getRoundCounter());
        assertEquals(1, state.getScore(1));
        assertWinner(0);
    }

    // ---------------- integration ----------------

    /**
     * Seeded random games with a low target (15). Oracle: when a player has 15 or more at the end, the game
     * ended at their scoring step, so they alone have reached it and they win. At least one seed must end early.
     */
    @Test
    public void seededGamesWithALowTargetEndWhenTheTargetIsReached() {
        int endedEarly = 0;
        for (long seed = 1; seed <= 10; seed++) {
            CribbageParameters p = new CribbageParameters();
            p.setParameterValue("targetScore", 15);
            Game game = newGame(seed, p);
            CribbageGameState gs = (CribbageGameState) game.getGameState();
            AbstractForwardModel gfm = game.getForwardModel();
            Random rnd = new Random(seed);
            int steps = 0;
            while (gs.isNotTerminal() && steps++ < 500) {
                List<AbstractAction> actions = gfm.computeAvailableActions(gs);
                assertFalse("no actions at step " + steps + " of seed " + seed, actions.isEmpty());
                gfm.next(gs, actions.get(rnd.nextInt(actions.size())));
                assertAllCardsPresent(gs);
            }
            assertFalse("seed " + seed + " did not end within 500 actions", gs.isNotTerminal());
            int s0 = gs.getScore(0), s1 = gs.getScore(1);
            if (s0 >= 15 || s1 >= 15) {
                int winner = s0 >= 15 ? 0 : 1;
                assertTrue("seed " + seed + ": both reached the target " + s0 + "/" + s1, gs.getScore(1 - winner) < 15);
                GameResult[] results = gs.getPlayerResults();
                assertEquals(WIN_GAME, results[winner]);
                assertEquals(LOSE_GAME, results[1 - winner]);
                if (gs.getRoundCounter() < 2) endedEarly++;
            }
        }
        assertTrue("no seed ended before the last round", endedEarly > 0);
    }
}
