package games.blackjack;

import core.actions.AbstractAction;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Naturals. By default (RECYCLE) a natural is an ordinary 21 and the player still plays. With
 * payout21NaturalOnly (Pagat, payout21 1.5) a natural is paid bet + floor(bet x payout21) as soon as the dealer is
 * known not to have Blackjack, its bet becomes 0, and that player takes no Play action; if every player has a
 * natural the dealer turns the hole card up, draws nothing, and the game ends.
 */
public class BlackjackNaturalsTest {

    static final Set<AbstractAction> HIT_OR_STAND = Set.of(new Hit(), new Stand());

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
        newState(2);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    private Set<AbstractAction> legalActions() {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    @Test
    public void aNaturalIsExactlyTwoCardsMakingTwentyOne() {
        giveHand(state, 0, "AH KC");
        giveHand(state, 1, "9S 8D");
        assertTrue(state.isNatural(0, 0));
        assertFalse(state.isNatural(1, 0));
        giveHand(state, 1, "10D AS");
        assertTrue("the order of the cards does not matter", state.isNatural(1, 0));
        giveHand(state, 0, "JH AC");
        assertTrue(state.isNatural(0, 0));
        giveHand(state, 0, "AH 5C 5D");     // 21 from three cards
        assertFalse(state.isNatural(0, 0));
        giveHand(state, 0, "KH QC");        // 20
        assertFalse(state.isNatural(0, 0));
        giveHand(state, 0, "AH AD");        // soft 12
        assertFalse(state.isNatural(0, 0));
    }

    @Test
    public void byDefaultAPlayerWithANaturalStillPlays() {
        // 7 + 10 = 17: no peek. Player 0 has a natural and is offered Hit or Stand; nothing is paid yet
        betAndDeal(state, fm, new int[]{2, 4}, new String[]{"AH KC", "10S 7D"}, "7S", "10D", "");
        assertEquals(Play, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());
        assertArrayEquals(new int[]{8, 6}, state.chips);
        assertEquals(2, state.getBet(0, 0));
    }

    @Test
    public void withNaturalOnlyANaturalIsPaidAtOnceAndThePlayerIsSkipped() {
        // the same deal as above: player 0's natural is paid 2 + floor(2 x 1.5) = 5 at once (8 + 5 = 13), and
        // player 1 plays first
        pagatNaturals(params);
        newState(2);
        betAndDeal(state, fm, new int[]{2, 4}, new String[]{"AH KC", "10S 7D"}, "7S", "10D", "");
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(HIT_OR_STAND, legalActions());
        assertEquals(13, state.getChips(0));
        assertEquals(0, state.getBet(0, 0));
        assertEquals(6, state.getChips(1));
        assertEquals(4, state.getBet(1, 0));
        assertEquals(setOf("AH KC"), setOf(state.getPlayerHand(0, 0)));
        assertEquals("the hole card is still face down", 1, state.getHoleCard().getSize());
        assertTrue(state.isNotTerminal());

        // player 1's 17 pushes against the dealer's 17; player 0 keeps the payout
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("7S 10D"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{13, 10}, state.chips);
        assertArrayEquals(new Object[]{WIN_GAME, DRAW_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void withNaturalOnlyPlayGoesToTheNextPlayerWithoutANatural() {
        pagatNaturals(params);
        newState(3);
        betAndDeal(state, fm, new int[]{2, 2, 2}, new String[]{"10H 5C", "AS QD", "9S 8D"}, "6S", "10D", "");
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(13, state.getChips(1));
        fm.next(state, new Stand());
        assertEquals("player 1's natural is skipped", 2, state.getCurrentPlayer());
        assertEquals(Play, state.getGamePhase());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void withNaturalOnlyANaturalIsPaidOnceAFailedPeekShowsNoDealerBlackjack() {
        // A + 2 is a soft 13, not a Blackjack. The only player has a natural, bet 4: 6 + 4 + floor(4 x 1.5) = 16.
        // Nobody is left to play: the hole card is turned up and the dealer does not draw (the 5 stays on the draw deck)
        pagatNaturals(params);
        newState(1);
        betAndDeal(state, fm, new int[]{4}, new String[]{"AH KC"}, "AS", "2D", "5H");
        declineInsurance(state, fm);     // every player offered insurance declines it
        assertFalse(state.isNotTerminal());
        assertEquals(16, state.getChips(0));
        assertEquals(0, state.getBet(0, 0));
        assertEquals(setOf("AS 2D"), setOf(state.getDealerHand()));
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(card("5H"), state.getDrawDeck().peek());
        assertArrayEquals(new Object[]{WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void withNaturalOnlyWhenEveryPlayerHasANaturalTheGameEndsAtOnce() {
        // 5 + 2 = 7, no peek. Bet 2 -> 8 + 2 + 3 = 13; bet 10 -> 0 + 10 + 15 = 25. The dealer does not draw
        pagatNaturals(params);
        newState(2);
        betAndDeal(state, fm, new int[]{2, 10}, new String[]{"AH KC", "QD AS"}, "5S", "2D", "9H");
        assertFalse(state.isNotTerminal());
        assertArrayEquals(new int[]{13, 25}, state.chips);
        assertEquals(0, state.getBet(0, 0));
        assertEquals(0, state.getBet(1, 0));
        assertEquals(setOf("5S 2D"), setOf(state.getDealerHand()));
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(card("9H"), state.getDrawDeck().peek());
        assertArrayEquals(new Object[]{WIN_GAME, WIN_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void withNaturalOnlyANaturalBeatsADealersThreeCardTwentyOne() {
        // dealer 6 + 5 = 11 draws the K: 21 from three cards, not a Blackjack. Player 0's natural has been paid
        // (13) and keeps it; player 1's 19 loses (bet 4: 6)
        pagatNaturals(params);
        betAndDeal(state, fm, new int[]{2, 4}, new String[]{"AH KC", "10C 9C"}, "6S", "5D", "KH");
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("6S 5D KH"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{13, 6}, state.chips);
        assertArrayEquals(new Object[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void withNaturalOnlyTheDealerDrawsNothingWhenEveryOtherHandIsBust() {
        // player 0's natural is paid at once; player 1 (10 + 6) hits the K and is bust. Every hand is settled, so
        // the dealer turns up 6 + 10 = 16 and does not draw the 5
        pagatNaturals(params);
        betAndDeal(state, fm, new int[]{2, 4}, new String[]{"AH KC", "10S 6D"}, "6S", "10D", "KH 5C");
        fm.next(state, new Hit());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("6S 10D"), setOf(state.getDealerHand()));
        assertEquals(card("5C"), state.getDrawDeck().peek());
        assertArrayEquals(new int[]{13, 6}, state.chips);
        assertArrayEquals(new Object[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }
}
