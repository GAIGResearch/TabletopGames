package games.blackjack;

import core.actions.AbstractAction;
import games.blackjack.actions.DoubleDown;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Split;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * The splitting option. In the Play phase Split is offered as well as Hit and Stand (and any
 * DoubleDown) when the active hand is exactly two cards of the same rank (K + Q is not a pair, 10 + 10 is), the player
 * has fewer than maxHandsAfterSplit hands and has chips of at least that hand's bet. A split inserts a new hand
 * straight after the active hand, holding the pair's second card, with an equal bet taken from the chips; each of
 * the two hands is then dealt one card from the draw deck, the active hand first, and play continues on the active hand.
 * Split Aces are both finished at once. Hands are played in order; after the last, the turn passes. After a split no
 * hand is a natural and there is no DoubleDown. Each hand is settled separately. Positions are arranged just after
 * the deal, with dealer up cards 2-9.
 */
public class BlackjackSplitTest {

    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());
    static final Set<AbstractAction> HIT_STAND_OR_SPLIT = Set.of(new Hit(), new Stand(), new Split());

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        params.setParameterValue("splitting", true);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    private Set<AbstractAction> legalActions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    /**
     * Three players, bets 2 each, on 8 + 8, 10 + 10 and K + Q against 6 + 7.
     */
    private void threePairs() {
        newState(3);
        arrangePlay(state, new int[]{2, 2, 2}, new String[]{"8H 8D", "10H 10C", "KH QS"}, "6S", "7D", "");
    }

    // ---------------------------------------------------------------- when it is offered

    @Test
    public void splitIsOfferedOnlyOnTwoCardsOfTheSameRank() {
        // 8 + 8 and 10 + 10 are pairs; K + Q (both worth 10) is not
        threePairs();
        assertEquals(HIT_STAND_OR_SPLIT, legalActions());
        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(HIT_STAND_OR_SPLIT, legalActions());
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());

        // A + A is a pair; 10 + K is not
        newState(2);
        arrangePlay(state, new int[]{2, 2}, new String[]{"AH AD", "10D KC"}, "6S", "7D", "");
        assertEquals(HIT_STAND_OR_SPLIT, legalActions());
        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void withSplittingOffAPairIsNotOffered() {
        // the pair of the test above
        params.setParameterValue("splitting", false);
        threePairs();
        assertEquals(HIT_OR_STAND, legalActions());
        fm.next(state, new Stand());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void aHandOfThreeCardsIsNotOfferedSplitEvenIfTwoOfThemArePaired() {
        // 2 + 5 hits the 5: 12 from three cards, two of them Fives
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"2H 5C"}, "6S", "7D", "5D");
        fm.next(state, new Hit());
        assertEquals(3, state.getPlayerHand(0, 0).getSize());
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void splitNeedsChipsOfAtLeastTheBet() {
        // bet 10 of 10: nothing left
        newState(1);
        arrangePlay(state, new int[]{10}, new String[]{"8H 8D"}, "6S", "7D", "");
        assertEquals(HIT_OR_STAND, legalActions());
        // bet 6 of 10: 4 left
        newState(1);
        arrangePlay(state, new int[]{6}, new String[]{"8H 8D"}, "6S", "7D", "");
        assertEquals(HIT_OR_STAND, legalActions());
        // bet 6 of 12: exactly 6 left
        params.setParameterValue("startingChips", 12);
        newState(1);
        arrangePlay(state, new int[]{6}, new String[]{"8H 8D"}, "6S", "7D", "");
        assertEquals(HIT_STAND_OR_SPLIT, legalActions());
    }

    @Test
    public void aResplitNeedsChipsForAnotherBet() {
        // bet 4 of 10: the split leaves 2, and the new pair 8 + 8 cannot be split again
        newState(1);
        arrangePlay(state, new int[]{4}, new String[]{"8H 8D"}, "6S", "7D", "8C 3D");
        fm.next(state, new Split());
        assertEquals(2, state.getChips(0));
        assertEquals(setOf("8H 8C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(HIT_OR_STAND, legalActions());
    }

    @Test
    public void maxHandsAfterSplitLimitsResplitting() {
        // 8 + 8 (bet 2 of 10) splits and gets the 8 and 3: hands 8 + 8, 8 + 3. Two hands: a resplit needs max > 2.
        // The resplit gives 8 + 8 again (the new hand 8 + 4): three hands, a further resplit needs max > 3
        for (int max : new int[]{2, 3, 4}) {
            params.setParameterValue("maxHandsAfterSplit", max);
            newState(1);
            arrangePlay(state, new int[]{2}, new String[]{"8H 8D"}, "6S", "7D", "8C 3D 8S 4H");
            assertEquals("max " + max, HIT_STAND_OR_SPLIT, legalActions());
            fm.next(state, new Split());
            assertEquals(2, state.getPlayerHands(0).size());
            assertEquals(setOf("8H 8C"), setOf(state.getPlayerHand(0, 0)));
            assertEquals("max " + max + ", 2 hands", max > 2 ? HIT_STAND_OR_SPLIT : HIT_OR_STAND, legalActions());
            if (max == 2)
                continue;
            fm.next(state, new Split());
            assertEquals(3, state.getPlayerHands(0).size());
            assertEquals(setOf("8H 8S"), setOf(state.getPlayerHand(0, 0)));
            assertEquals("max " + max + ", 3 hands", max > 3 ? HIT_STAND_OR_SPLIT : HIT_OR_STAND, legalActions());
        }
    }

    @Test
    public void splitAndDoubleDownAreBothOfferedOnAFirstPairButNoDoubleDownAfterASplit() {
        // doubleDown on: 5 + 5 (bet 2 of 10) may double or split. Split, getting the 6 and the 4: 5 + 6 = 11 and
        // 5 + 4 = 9 are the classic doubling totals, but a split hand may not double
        params.setParameterValue("doubleDown", true);
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"5H 5D"}, "6S", "7D", "6C 4S");
        assertEquals(Set.of(new Hit(), new Stand(), new DoubleDown(), new Split()), legalActions());
        fm.next(state, new Split());
        assertEquals(setOf("5H 6C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(HIT_OR_STAND, legalActions());
        fm.next(state, new Stand());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getActiveHand());
        assertEquals(setOf("5D 4S"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(HIT_OR_STAND, legalActions());
    }

    // ---------------------------------------------------------------- what it does

    @Test
    public void splittingMovesTheSecondCardToANewHandWithAnEqualBet() {
        // player 0 (bet 4, 6 left) splits 8H + 8D (the 8D dealt second): the 8H hand gets the 3C, the new 8D hand
        // the 5D, and a second bet of 4 leaves 2 chips
        newState(2);
        arrangePlay(state, new int[]{4, 2}, new String[]{"8H 8D", "9S 7C"}, "6S", "10D", "3C 5D 9H");
        int drawDeckSize = state.getDrawDeck().getSize();
        fm.next(state, new Split());
        assertEquals(2, state.getPlayerHands(0).size());
        assertEquals(setOf("8H 3C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("8D 5D"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(List.of(4, 4), state.bets.get(0));
        assertEquals(2, state.getChips(0));
        assertEquals(drawDeckSize - 2, state.getDrawDeck().getSize());
        assertEquals(card("9H"), state.getDrawDeck().peek());
        // player 0 goes on with the first hand
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(HIT_OR_STAND, legalActions());
        // nothing else changed
        assertEquals(1, state.getPlayerHands(1).size());
        assertEquals(setOf("9S 7C"), setOf(state.getPlayerHand(1, 0)));
        assertEquals(List.of(2), state.bets.get(1));
        assertEquals(8, state.getChips(1));
        assertEquals(setOf("6S"), setOf(state.getDealerHand()));
        assertEquals(1, state.getHoleCard().getSize());
        assertAllCardsPresent(state);
    }

    /**
     * Two players, bets 2, dealer 6 + 10. Player 0 splits 8 + 8, gets 8S and 3C (8 + 8, 8 + 3), and resplits the
     * first hand: the new hand (8S) goes in straight after it, before the 8 + 3. The first hand gets the 5D and the
     * new hand the 2H: hands 8H + 5D, 8S + 2H, 8D + 3C. Then 10S, 9D and KH are on top of the draw deck.
     */
    private void splitAndResplit() {
        newState(2);
        arrangePlay(state, new int[]{2, 2}, new String[]{"8H 8D", "9S 7C"}, "6S", "10D", "8S 3C 5D 2H 10S 9D KH");
        fm.next(state, new Split());
        assertEquals(setOf("8H 8S"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("8D 3C"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(6, state.getChips(0));
        assertEquals(HIT_STAND_OR_SPLIT, legalActions());
        fm.next(state, new Split());
    }

    @Test
    public void aResplitHandGoesStraightAfterTheHandBeingPlayed() {
        splitAndResplit();
        assertEquals(3, state.getPlayerHands(0).size());
        assertEquals(setOf("8H 5D"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("8S 2H"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(setOf("8D 3C"), setOf(state.getPlayerHand(0, 2)));
        assertEquals(List.of(2, 2, 2), state.bets.get(0));
        assertEquals(4, state.getChips(0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(card("10S"), state.getDrawDeck().peek());
        assertAllCardsPresent(state);
    }

    @Test
    public void handsArePlayedInOrderAndThenTheTurnPasses() {
        splitAndResplit();
        // hand 0: 8 + 5 hits the 10: 23, bust - on to hand 1
        fm.next(state, new Hit());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getActiveHand());
        assertEquals(HIT_OR_STAND, legalActions());
        // hand 1: 8 + 2 hits the 9: 19, still hand 1; Stand - on to hand 2
        fm.next(state, new Hit());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getActiveHand());
        assertEquals(setOf("8S 2H 9D"), setOf(state.getPlayerHand(0, 1)));
        fm.next(state, new Stand());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(2, state.getActiveHand());
        // hand 2: 8 + 3 stands - the turn passes to player 1, on their first hand
        fm.next(state, new Stand());
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(HIT_OR_STAND, legalActions());

        // player 1 stands on 16; the dealer's 16 draws the K: bust. Hand 0 (bust) loses; hands 1 and 2 win 2 each:
        // 4 + 4 + 4 = 12. Player 1 wins 2: 8 + 4 = 12
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("6S 10D KH"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{12, 12}, state.chips);
        assertEquals(List.of(0, 0, 0), state.bets.get(0));
        assertArrayEquals(new Object[]{WIN_GAME, WIN_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    // ---------------------------------------------------------------- split Aces

    @Test
    public void splitAcesGetOneCardEachAndThePlayPassesAtOnce() {
        // player 0 (bet 2, 8 left) splits A + A: A + 5 and A + K, and takes no more actions on either
        newState(2);
        arrangePlay(state, new int[]{2, 2}, new String[]{"AH AD", "9S 7C"}, "6S", "10D", "5C KD 4H");
        fm.next(state, new Split());
        assertEquals(2, state.getPlayerHands(0).size());
        assertEquals(setOf("AH 5C"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("AD KD"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(List.of(2, 2), state.bets.get(0));
        assertEquals(6, state.getChips(0));
        assertEquals(card("4H"), state.getDrawDeck().peek());
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(HIT_OR_STAND, legalActions());
        // A + K after a split is 21 but not a natural
        assertFalse(state.isNatural(0, 1));
    }

    @Test
    public void splitAcesThatDrawAnotherAceAreNotResplit() {
        // A + A splits and the first hand gets the AC: A + A again, but split Aces are finished
        newState(2);
        arrangePlay(state, new int[]{2, 2}, new String[]{"AH AD", "9S 7C"}, "6S", "10D", "AC 9D");
        fm.next(state, new Split());
        assertEquals(2, state.getPlayerHands(0).size());
        assertEquals(setOf("AH AC"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("AD 9D"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
    }

    // ---------------------------------------------------------------- 21 after a split is not a natural

    /**
     * One player (bet 2, 8 left) splits A + A, getting the 5 and the K: A + 5 (soft 16) and A + K (21, not a
     * natural). Play passes at once, so the dealer's 9 + 8 = 17 stands and the hands are settled. Returns the chips.
     */
    private int oneSplitOfAces() {
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"AH AD"}, "9S", "8D", "5C KD 4H");
        fm.next(state, new Split());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("9S 8D"), setOf(state.getDealerHand()));
        assertEquals(List.of(0, 0), state.bets.get(0));
        return state.getChips(0);
    }

    @Test
    public void byDefaultATwentyOneOfSplitAcesIsPaidPayout21() {
        // soft 16 loses; the 21 wins floor(2 x 2.0) = 4: 6 + 2 + 4 = 12
        assertEquals(12, oneSplitOfAces());
    }

    @Test
    public void withNaturalOnlyATwentyOneOfSplitAcesIsPaidEvenMoney() {
        // the pair of the test above: not a natural, so 1:1: 6 + 2 + 2 = 10
        pagatNaturals(params);
        assertEquals(10, oneSplitOfAces());
    }

    /**
     * One player (bet 2, 8 left) splits 10 + 10, getting the A and the 7: 10 + A (a two-card 21) and 10 + 7, and
     * stands on each. The dealer has 6S and the hole card; the draw deck then holds dealerDraws.
     */
    private void splitTensToTwentyOne(String hole, String dealerDraws) {
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"10H 10D"}, "6S", hole, "AC 7C " + dealerDraws);
        fm.next(state, new Split());
        // not paid at once, even with NaturalOnly: the player plays the 21 like any other hand
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getActiveHand());
        assertEquals(setOf("10H AC"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(List.of(2, 2), state.bets.get(0));
        assertEquals(6, state.getChips(0));
        assertFalse(state.isNatural(0, 0));
        assertEquals(HIT_OR_STAND, legalActions());
        fm.next(state, new Stand());
        assertEquals(1, state.getActiveHand());
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
    }

    @Test
    public void byDefaultATwoCardTwentyOneAfterASplitIsPaidPayout21() {
        // dealer 6 + Q = 16 draws the 2: 18. 21 wins floor(2 x 2.0) = 4; 17 loses: 6 + 2 + 4 = 12
        splitTensToTwentyOne("QD", "2S");
        assertEquals(12, state.getChips(0));
    }

    @Test
    public void withNaturalOnlyATwoCardTwentyOneAfterASplitIsAnOrdinaryTwentyOne() {
        // the pair of the test above: it is played on (not paid at once) and wins 1:1: 6 + 2 + 2 = 10
        pagatNaturals(params);
        splitTensToTwentyOne("QD", "2S");
        assertEquals(10, state.getChips(0));
    }

    @Test
    public void withNaturalOnlyATwoCardTwentyOneAfterASplitOnlyPushesADealerTwentyOne() {
        // dealer 6 + 5 draws the 10: 21 of three cards. A natural would beat it; the split 21 pushes (bet back) and
        // 17 loses: 6 + 2 = 8
        pagatNaturals(params);
        splitTensToTwentyOne("5D", "10S");
        assertEquals(setOf("6S 5D 10S"), setOf(state.getDealerHand()));
        assertEquals(8, state.getChips(0));
    }

    // ---------------------------------------------------------------- settlement

    @Test
    public void eachHandIsSettledSeparately() {
        // 8 + 8 (bet 2, 8 left) splits: 8 + 8 and 8 + 10; resplits the first: 8 + A (19) and 8 + 9 (17), before
        // 8 + 10 (18). Stand on each. The dealer's 10 + 8 = 18 stands: 19 wins, 17 loses, 18 pushes: 4 + 4 + 2 = 10
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"8H 8D"}, "10S", "8S", "8C 10C AS 9H 2D");
        fm.next(state, new Split());
        fm.next(state, new Split());
        assertEquals(setOf("8H AS"), setOf(state.getPlayerHand(0, 0)));
        assertEquals(setOf("8C 9H"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(setOf("8D 10C"), setOf(state.getPlayerHand(0, 2)));
        assertEquals(4, state.getChips(0));
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("10S 8S"), setOf(state.getDealerHand()));
        assertEquals(10, state.getChips(0));
        assertEquals(List.of(0, 0, 0), state.bets.get(0));
        assertArrayEquals(new Object[]{DRAW_GAME}, state.getPlayerResults());
    }

    /**
     * One player (bet 2, 8 left) splits 10 + 10 into 10 + 6 and 10 + 5, and hits the first to 26 (bust) with the K.
     * The dealer has 10S + 6D (16); the draw deck then holds the QS and the 6H.
     */
    private void splitTensAndBustTheFirst() {
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"10H 10D"}, "10S", "6D", "6C 5C KS QS 6H");
        fm.next(state, new Split());
        fm.next(state, new Hit());
        assertEquals(1, state.getActiveHand());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void theDealerDoesNotDrawWhenEverySplitHandIsBust() {
        // the second hand hits the Q: 25, bust. Both bets lost: 6; the dealer's 16 does not draw the 6
        splitTensAndBustTheFirst();
        fm.next(state, new Hit());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("10S 6D"), setOf(state.getDealerHand()));
        assertEquals(setOf("10D 5C QS"), setOf(state.getPlayerHand(0, 1)));
        assertEquals(card("6H"), state.getDrawDeck().peek());
        assertEquals(6, state.getChips(0));
    }

    @Test
    public void theDealerDrawsWhenAnySplitHandIsLive() {
        // the second hand stands on 15: the dealer's 16 draws the Q: bust. 15 wins 2, the bust hand loses: 6 + 4 = 10
        splitTensAndBustTheFirst();
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("10S 6D QS"), setOf(state.getDealerHand()));
        assertEquals(10, state.getChips(0));
    }

    // ---------------------------------------------------------------- the next hand

    @Test
    public void theExtraHandsAreRemovedAtTheNextHand() {
        // nHands 2. 8 + 8 splits (8 + 8, 8 + 3) and resplits (8 + 2, 8 + 4, 8 + 3); stand on each. The dealer's 16
        // draws the 10: bust, and all three hands win 2: 4 + 12 = 16. The next hand has one empty hand with bet 0
        params.setParameterValue("nHands", 2);
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"8H 8D"}, "6S", "10D", "8C 3D 2S 4H 10H");
        fm.next(state, new Split());
        fm.next(state, new Split());
        assertEquals(3, state.getPlayerHands(0).size());
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        assertNewHandStarted(state, 1, 0);
        assertEquals(List.of(0), state.bets.get(0));
        assertEquals(16, state.getChips(0));
        assertEquals(betsUpTo(16), legalActions());
    }

    // ---------------------------------------------------------------- copies

    @Test
    public void aCopyWithSeveralHandsIsEqualAndDeep() {
        // after the split and resplit, hand 0 busts: player 0 is on hand 1 of 3
        splitAndResplit();
        fm.next(state, new Hit());
        assertEquals(1, state.getActiveHand());

        BlackjackGameState copy = (BlackjackGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(1, copy.getActiveHand());
        assertEquals(List.of(2, 2, 2), copy.bets.get(0));
        for (int h = 0; h < 3; h++)
            assertEquals(state.getPlayerHand(0, h).getComponents(), copy.getPlayerHand(0, h).getComponents());

        // playing on in the copy leaves the original alone
        int originalHash = state.hashCode();
        fm.next(copy, new Hit());
        assertEquals(3, copy.getPlayerHand(0, 1).getSize());
        assertEquals(2, state.getPlayerHand(0, 1).getSize());
        copy.bets.get(0).set(2, 8);
        copy.getPlayerHand(0, 2).add(copy.getDrawDeck().draw());
        assertEquals(originalHash, state.hashCode());
        assertEquals(List.of(2, 2, 2), state.bets.get(0));
        assertEquals(setOf("8D 3C"), setOf(state.getPlayerHand(0, 2)));
        assertNotEquals(state, copy);

        // a second hand's bet is part of equals
        BlackjackGameState other = (BlackjackGameState) state.copy();
        other.bets.get(0).set(2, 4);
        assertNotEquals(state, other);

        // redeterminised from player 1: every hand, bet and the active hand are kept; the draw deck is only shuffled
        BlackjackGameState view = (BlackjackGameState) state.copy(1);
        assertEquals(3, view.getPlayerHands(0).size());
        for (int h = 0; h < 3; h++)
            assertEquals(state.getPlayerHand(0, h).getComponents(), view.getPlayerHand(0, h).getComponents());
        assertEquals(state.bets, view.bets);
        assertArrayEquals(state.chips, view.chips);
        assertEquals(1, view.getActiveHand());
        assertEquals(0, view.getCurrentPlayer());
        assertEquals(state.getDrawDeck().getSize(), view.getDrawDeck().getSize());
        assertAllCardsPresent(view);
    }
}
