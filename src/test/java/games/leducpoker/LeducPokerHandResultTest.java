package games.leducpoker;

import core.CoreConstants;
import games.leducpoker.actions.Call;
import games.leducpoker.actions.Fold;
import games.leducpoker.actions.Raise;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.leducpoker.LeducPokerTestUtils.*;
import static org.junit.Assert.*;

/**
 * Folds, the showdown comparison and the settlement of a single hand (nHands = 1, so the game ends with it).
 */
public class LeducPokerHandResultTest {

    LeducPokerParameters params;
    LeducPokerGameState state;
    LeducPokerForwardModel fm;

    static final Call CALL = new Call();
    static final Raise RAISE = new Raise();
    static final Fold FOLD = new Fold();

    @Before
    public void setup() {
        params = new LeducPokerParameters();
        params.setRandomSeed(42);
        state = new LeducPokerGameState(params, 2);
        fm = new LeducPokerForwardModel();
        fm.setup(state);
    }

    private void assertResult(int p, CoreConstants.GameResult expected) {
        assertEquals("result of player " + p, expected, state.getPlayerResults()[p]);
    }

    private void assertNet(int net0, int net1) {
        assertEquals(net0, state.getNetChips(0));
        assertEquals(net1, state.getNetChips(1));
        assertEquals(net0, state.getGameScore(0), 1e-9);
        assertEquals(net1, state.getGameScore(1), 1e-9);
    }

    // ---- LeducPokerUtils ----

    @Test
    public void rankValueIsJackQueenKingFromZero() {
        assertEquals(0, LeducPokerUtils.rankValue(card("JS")));
        assertEquals(0, LeducPokerUtils.rankValue(card("JH")));
        assertEquals(1, LeducPokerUtils.rankValue(card("QH")));
        assertEquals(2, LeducPokerUtils.rankValue(card("KS")));
    }

    @Test
    public void aPairWithTheBoardBeatsAHigherCard() {
        assertEquals(0, LeducPokerUtils.showdownWinner(card("JS"), card("KS"), card("JH"), false));
        assertEquals(1, LeducPokerUtils.showdownWinner(card("KS"), card("QS"), card("QH"), false));
    }

    @Test
    public void withNoPairTheHigherPrivateCardWins() {
        assertEquals(0, LeducPokerUtils.showdownWinner(card("QS"), card("JS"), card("KH"), false));
        assertEquals(1, LeducPokerUtils.showdownWinner(card("JS"), card("KS"), card("QH"), false));
        assertEquals(1, LeducPokerUtils.showdownWinner(card("QS"), card("KS"), card("JH"), false));
    }

    @Test
    public void equalRanksWithoutAPairTie() {
        assertEquals(-1, LeducPokerUtils.showdownWinner(card("QS"), card("QH"), card("KH"), false));
        assertEquals(-1, LeducPokerUtils.showdownWinner(card("KS"), card("KH"), card("JS"), false));
    }

    // ---- LeducPokerUtils with highCardUsesBoard (RECYCLE variant) ----

    @Test
    public void highCardUsesBoardTiesQueenAndJackUnderAKingBoard() {
        // max(Q, K) = K = max(J, K)
        assertEquals(-1, LeducPokerUtils.showdownWinner(card("QS"), card("JS"), card("KH"), true));
        assertEquals(-1, LeducPokerUtils.showdownWinner(card("JH"), card("QH"), card("KS"), true));
    }

    @Test
    public void highCardUsesBoardStillLetsTheHigherCardWinWhenItBeatsTheBoard() {
        // board J: max(K, J) = K beats max(Q, J) = Q
        assertEquals(1, LeducPokerUtils.showdownWinner(card("QS"), card("KS"), card("JH"), true));
        assertEquals(0, LeducPokerUtils.showdownWinner(card("KS"), card("QS"), card("JH"), true));
        // board Q: max(K, Q) = K beats max(J, Q) = Q
        assertEquals(0, LeducPokerUtils.showdownWinner(card("KS"), card("JS"), card("QH"), true));
        assertEquals(1, LeducPokerUtils.showdownWinner(card("JH"), card("KH"), card("QS"), true));
    }

    @Test
    public void highCardUsesBoardKeepsPairsOnTop() {
        // J pairs board J against K; Q pairs board Q against K
        assertEquals(0, LeducPokerUtils.showdownWinner(card("JS"), card("KS"), card("JH"), true));
        assertEquals(1, LeducPokerUtils.showdownWinner(card("KS"), card("QS"), card("QH"), true));
    }

    @Test
    public void highCardUsesBoardTiesEqualRanks() {
        assertEquals(-1, LeducPokerUtils.showdownWinner(card("QS"), card("QH"), card("KH"), true));
        assertEquals(-1, LeducPokerUtils.showdownWinner(card("JS"), card("JH"), card("QS"), true));
    }

    // ---- fold ----

    @Test
    public void foldInTheFirstRoundLosesTheFoldersContributionAndEndsTheGame() {
        // player 1 holds the better card, so a showdown would go the other way
        arrange(state, card("JS"), card("KS"));
        play(fm, state, RAISE, FOLD);
        // player 0 contributed 1 + 2 = 3, player 1 only the ante 1
        assertNet(1, -1);
        assertFalse(state.isNotTerminal());
        assertResult(0, WIN_GAME);
        assertResult(1, LOSE_GAME);
        assertEquals("no board card is dealt after a fold", 0, state.getBoard().getSize());
        assertEquals(4, state.getDrawDeck().getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void foldInTheSecondRoundSettlesOnlyTheFoldersContribution() {
        arrange(state, card("KS"), card("JS"), card("QH"));
        // round 0: P0 raise (3), P1 re-raise (1 + 2 + 2 = 5), P0 call (5); round 1: P0 raise (5 + 4 = 9), P1 folds
        play(fm, state, RAISE, RAISE, CALL, RAISE, FOLD);
        // player 1 loses own 5; player 0 gains 5 (not the pot of 14, nor its own 9)
        assertNet(5, -5);
        assertResult(0, WIN_GAME);
        assertResult(1, LOSE_GAME);
        assertEquals(1, state.getBoard().getSize());
    }

    @Test
    public void playerZeroCanFoldToo() {
        arrange(state, card("KS"), card("JS"));
        // P0 check, P1 raise (3), P0 folds with ante 1 in
        play(fm, state, CALL, RAISE, FOLD);
        assertNet(-1, 1);
        assertResult(0, LOSE_GAME);
        assertResult(1, WIN_GAME);
    }

    // ---- showdown through the forward model ----

    @Test
    public void pairingTheBoardWinsAtShowdown() {
        // P0 JS pairs the board JH; P1 has the higher card KS
        arrange(state, card("JS"), card("KS"), card("JH"));
        // round 0 check-check (1/1); round 1 P0 raise (1 + 4 = 5), P1 call (5)
        play(fm, state, CALL, CALL, RAISE, CALL);
        assertFalse(state.isNotTerminal());
        assertNet(5, -5);
        assertResult(0, WIN_GAME);
        assertResult(1, LOSE_GAME);
    }

    @Test
    public void higherPrivateCardWinsAtShowdownWithNoPair() {
        arrange(state, card("QS"), card("KS"), card("JH"));
        // round 0 P0 raise (3), P1 call (3); round 1 check-check
        play(fm, state, RAISE, CALL, CALL, CALL);
        assertFalse(state.isNotTerminal());
        assertNet(-3, 3);
        assertResult(0, LOSE_GAME);
        assertResult(1, WIN_GAME);
    }

    @Test
    public void equalRanksTieAndNoChipsChangeHands() {
        arrange(state, card("QS"), card("QH"), card("KH"));
        // contributions 3 / 3 after raise-call, then check-check
        play(fm, state, RAISE, CALL, CALL, CALL);
        assertFalse(state.isNotTerminal());
        assertNet(0, 0);
        assertResult(0, DRAW_GAME);
        assertResult(1, DRAW_GAME);
    }

    @Test
    public void byDefaultQueenBeatsJackUnderAKingBoard() {
        arrange(state, card("QS"), card("JS"), card("KH"));
        // round 0 raise-call (3 / 3), round 1 check-check; Q beats J on the private cards
        play(fm, state, RAISE, CALL, CALL, CALL);
        assertFalse(state.isNotTerminal());
        assertNet(3, -3);
        assertResult(0, WIN_GAME);
        assertResult(1, LOSE_GAME);
    }

    @Test
    public void withHighCardUsesBoardQueenAndJackTieUnderAKingBoard() {
        params.setParameterValue("highCardUsesBoard", true);
        state = new LeducPokerGameState(params, 2);
        fm.setup(state);
        arrange(state, card("QS"), card("JS"), card("KH"));
        // same play as the default case: both best cards are the board K, so no chips change hands
        play(fm, state, RAISE, CALL, CALL, CALL);
        assertFalse(state.isNotTerminal());
        assertNet(0, 0);
        assertResult(0, DRAW_GAME);
        assertResult(1, DRAW_GAME);
    }

    @Test
    public void withHighCardUsesBoardKingStillBeatsQueenUnderAJackBoard() {
        params.setParameterValue("highCardUsesBoard", true);
        state = new LeducPokerGameState(params, 2);
        fm.setup(state);
        arrange(state, card("QS"), card("KS"), card("JH"));
        // round 0 check-check (1 / 1); round 1 P0 bet (1 + 4 = 5), P1 call (5); K beats Q
        play(fm, state, CALL, CALL, RAISE, CALL);
        assertFalse(state.isNotTerminal());
        assertNet(-5, 5);
        assertResult(0, LOSE_GAME);
        assertResult(1, WIN_GAME);
    }

    @Test
    public void largestPotIsThirteenEach() {
        arrange(state, card("KS"), card("QS"), card("JH"));
        // round 0: raise (P0 3), re-raise (P1 1 + 2 + 2 = 5), call (P0 5)
        // round 1: raise (P0 5 + 4 = 9), re-raise (P1 5 + 4 + 4 = 13), call (P0 13)
        play(fm, state, RAISE, RAISE, CALL, RAISE, RAISE, CALL);
        assertEquals(13, state.getContribution(0));
        assertEquals(13, state.getContribution(1));
        assertEquals("1 + 2 * (2 + 4)", 13, params.maxContribution());
        assertFalse(state.isNotTerminal());
        // K beats Q, no pair with J
        assertNet(13, -13);
    }

    // ---- heuristic ----

    @Test
    public void heuristicStaysInTheUnitIntervalThroughToTheEndOfTheGame() {
        arrange(state, card("KS"), card("QS"), card("JH"));
        assertHeuristicInRange();
        play(fm, state, RAISE, RAISE, CALL);
        assertHeuristicInRange();
        play(fm, state, RAISE, RAISE, CALL);
        assertFalse(state.isNotTerminal());
        assertHeuristicInRange();
        assertTrue("winner above loser", state.getHeuristicScore(0) > state.getHeuristicScore(1));
    }

    private void assertHeuristicInRange() {
        for (int p = 0; p < 2; p++) {
            double h = state.getHeuristicScore(p);
            assertTrue("heuristic " + h + " for player " + p, h >= 0.0 && h <= 1.0);
        }
    }
}
