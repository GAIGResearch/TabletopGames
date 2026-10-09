package games.leducpoker;

import core.components.FrenchCard;
import games.leducpoker.actions.Call;
import games.leducpoker.actions.Fold;
import games.leducpoker.actions.Raise;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.leducpoker.LeducPokerTestUtils.*;
import static org.junit.Assert.*;

/**
 * Matches of several hands (nHands > 1): the next deal, the alternating first player, net chips carried over, and
 * the match result by total net chips. Driven through fm.next; each hand is arranged with arrange(...) at its start.
 */
public class LeducPokerMatchTest {

    static final Call CALL = new Call();
    static final Raise RAISE = new Raise();
    static final Fold FOLD = new Fold();

    LeducPokerParameters params;
    LeducPokerGameState state;
    LeducPokerForwardModel fm;

    @Before
    public void setup() {
        newMatch(3);
    }

    private void newMatch(int nHands) {
        params = new LeducPokerParameters();
        params.setRandomSeed(42);
        params.setParameterValue("nHands", nHands);
        state = new LeducPokerGameState(params, 2);
        fm = new LeducPokerForwardModel();
        fm.setup(state);
    }

    /**
     * The state at the start of a hand after handsFinished hands: fresh deal, antes, counters reset, net chips kept.
     */
    private void assertNewHand(int handsFinished, int firstPlayer, int net0) {
        assertTrue("match over after " + handsFinished + " hands", state.isNotTerminal());
        assertEquals("round counter = hands finished", handsFinished, state.getRoundCounter());
        assertEquals("first player", firstPlayer, state.getFirstPlayer());
        assertEquals("first player acts first", firstPlayer, state.getCurrentPlayer());
        for (int p = 0; p < 2; p++) {
            assertEquals("one card for player " + p, 1, state.getHand(p).getSize());
            assertEquals("ante of player " + p, 1, state.getContribution(p));
            assertEquals(GAME_ONGOING, state.getPlayerResults()[p]);
        }
        assertEquals(0, state.getBoard().getSize());
        assertEquals(4, state.getDrawDeck().getSize());
        assertEquals(0, state.getRaisesThisRound());
        assertEquals(0, state.getActionsThisRound());
        assertEquals(net0, state.getNetChips(0));
        assertEquals(-net0, state.getNetChips(1));
        assertEquals(List.of(CALL, RAISE), fm.computeAvailableActions(state));
        assertAllCardsPresent(state);
    }

    @Test
    public void aFoldEndsTheHandButNotTheMatch() {
        arrange(state, card("KS"), card("JS"));
        // P0 raise (3), P1 folds with the ante 1 in
        play(fm, state, RAISE, FOLD);
        assertNewHand(1, 1, 1);
    }

    @Test
    public void aShowdownEndsTheHandAndTheBoardCardGoesBackForTheNextDeal() {
        arrange(state, card("KS"), card("JS"), card("QH"));
        // check-check twice; K beats J with board Q, player 1's ante 1
        play(fm, state, CALL, CALL, CALL, CALL);
        assertNewHand(1, 1, 1);
    }

    @Test
    public void scriptedMatchCarriesNetChipsAndIsWonOnTheTotal() {
        // hand 1, P0 first: KS v JS, board QH
        arrange(state, card("KS"), card("JS"), card("QH"));
        // round 0 P0 raise (3), P1 call (3); round 1 check-check; K beats J: P0 +3
        play(fm, state, RAISE, CALL, CALL, CALL);
        assertNewHand(1, 1, 3);

        // hand 2, P1 first: QS v JS, board QH
        arrange(state, card("QS"), card("JS"), card("QH"));
        // round 0 P1 check, P0 check
        play(fm, state, CALL, CALL);
        assertEquals(List.of(card("QH")), state.getBoard().getComponents());
        assertEquals("the hand's first player opens round 1 too", 1, state.getCurrentPlayer());
        // round 1 P1 bet (1 + 4 = 5), P0 call (5); P0's Q pairs the board: P0 +5, total 3 + 5 = 8
        fm.next(state, RAISE);
        assertEquals(5, state.getContribution(1));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, CALL);
        assertNewHand(2, 0, 8);

        // hand 3, P0 first: JS v KS, board QH
        arrange(state, card("JS"), card("KS"), card("QH"));
        // round 0 raise (P0 3), re-raise (P1 5), call (P0 5); round 1 raise (P0 9), re-raise (P1 13), call (P0 13)
        play(fm, state, RAISE, RAISE, CALL, RAISE, RAISE, CALL);
        // K beats J with board Q: P1 +13. Totals: P0 8 - 13 = -5, P1 -8 + 13 = 5
        assertFalse("the match ends after the third hand", state.isNotTerminal());
        assertEquals("no fourth hand is started", 2, state.getRoundCounter());
        assertEquals(-5, state.getNetChips(0));
        assertEquals(5, state.getNetChips(1));
        assertEquals(-5, state.getGameScore(0), 1e-9);
        // player 0 won two hands of three, but player 1 has more chips
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
        // the last hand's cards are left where they are
        assertEquals(List.of(card("QH")), state.getBoard().getComponents());
        assertAllCardsPresent(state);
    }

    @Test
    public void equalTotalsAreADrawAndTheMatchLastsExactlyNHands() {
        newMatch(2);
        // hand 1, P0 first: P0 raise, P1 folds its ante: +1 / -1
        play(fm, state, RAISE, FOLD);
        assertNewHand(1, 1, 1);
        // hand 2, P1 first: P1 raise, P0 folds its ante: 0 / 0
        play(fm, state, RAISE, FOLD);
        assertFalse(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getNetChips(0));
        assertEquals(0, state.getNetChips(1));
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(DRAW_GAME, state.getPlayerResults()[1]);
        assertEquals(0.5, state.getHeuristicScore(0), 1e-9);
    }

    @Test
    public void firstPlayerAlternatesOverFourHands() {
        newMatch(4);
        int net0 = 0;
        for (int hand = 0; hand < 4; hand++) {
            int first = hand % 2;
            assertNewHand(hand, first, net0);
            if (first == 0)
                play(fm, state, RAISE, FOLD);        // P0 raise, P1 folds its ante
            else
                play(fm, state, CALL, RAISE, FOLD);  // P1 check, P0 raise, P1 folds its ante
            net0 += 1;
        }
        assertFalse(state.isNotTerminal());
        assertEquals(3, state.getRoundCounter());
        assertEquals(4, state.getNetChips(0));
        assertEquals(-4, state.getNetChips(1));
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void theNextHandIsShuffledWithTheStatesRandomGenerator() {
        arrange(state, card("KS"), card("JS"));
        // faithful copies are equal but each has its own generator; play the same hand in each
        Set<List<FrenchCard>> deals = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            LeducPokerGameState copy = (LeducPokerGameState) state.copy();
            assertEquals(state, copy);
            play(fm, copy, RAISE, FOLD);
            assertEquals(1, copy.getRoundCounter());
            List<FrenchCard> deal = new ArrayList<>();
            deal.addAll(copy.getHand(0).getComponents());
            deal.addAll(copy.getHand(1).getComponents());
            deal.addAll(copy.getDrawDeck().getComponents());
            assertEquals(6, deal.size());
            deals.add(deal);
        }
        assertTrue("the same next deal every time: " + deals, deals.size() > 1);
    }

    @Test
    public void copyInALaterHandKeepsTheRoundCounterAndFirstPlayer() {
        play(fm, state, RAISE, FOLD);
        assertNewHand(1, 1, 1);
        arrange(state, card("QS"), card("KS"), card("JH"));
        // hand 2: P1 raise (3); P0 to act
        fm.next(state, RAISE);

        LeducPokerGameState copy = (LeducPokerGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(1, copy.getRoundCounter());
        assertEquals(1, copy.getFirstPlayer());
        assertEquals(0, copy.getCurrentPlayer());

        LeducPokerGameState det = (LeducPokerGameState) state.copy(0);
        assertEquals(1, det.getRoundCounter());
        assertEquals(1, det.getFirstPlayer());
        assertEquals(0, det.getCurrentPlayer());
        assertEquals(List.of(card("QS")), det.getHand(0).getComponents());
        assertEquals(1, det.getNetChips(0));
        assertEquals(3, det.getContribution(1));
        assertAllCardsPresent(det);

        // the copy and the original carry on the same way: P0 calls, round 1 opens with player 1
        fm.next(state, CALL);
        fm.next(copy, CALL);
        assertEquals(state, copy);
        assertEquals(1, copy.getCurrentPlayer());
        assertEquals(List.of(card("JH")), copy.getBoard().getComponents());
    }
}
