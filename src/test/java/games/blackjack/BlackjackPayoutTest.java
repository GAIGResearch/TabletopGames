package games.blackjack;

import games.blackjack.actions.Hit;
import games.blackjack.actions.Stand;
import org.junit.Before;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Settlement of 21s. By default (RECYCLE) any 21 that beats the dealer wins
 * floor(bet x payout21) instead of 1:1, and a 21 that ties the dealer's 21 is a push. With payout21NaturalOnly only a
 * natural gets payout21 (paid at once, see BlackjackNaturalsTest), and every other winning 21 pays 1:1.
 * Positions are arranged just after the deal with no dealer Blackjack.
 */
public class BlackjackPayoutTest {

    BlackjackParameters params;
    BlackjackGameState state;
    BlackjackForwardModel fm;

    @Before
    public void setup() {
        params = new BlackjackParameters();
        params.setRandomSeed(42);
    }

    private void newState(int nPlayers) {
        state = new BlackjackGameState(params, nPlayers);
        fm = new BlackjackForwardModel();
        fm.setup(state);
    }

    /**
     * One player, the given bet, 10 + 5 hits the 6 to make 21 from three cards and stands against the dealer's
     * 9 + 10 = 19. Returns the player's chips at the end.
     */
    private int threeCardTwentyOneBeatsNineteen(int bet) {
        newState(1);
        arrangePlay(state, new int[]{bet}, new String[]{"10C 5C"}, "9S", "10D", "6H");
        fm.next(state, new Hit());
        assertEquals(setOf("10C 5C 6H"), setOf(state.getPlayerHand(0, 0)));
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("9S 10D"), setOf(state.getDealerHand()));
        assertEquals(0, state.getBet(0, 0));
        assertArrayEquals(new Object[]{WIN_GAME}, state.getPlayerResults());
        return state.getChips(0);
    }

    @Test
    public void byDefaultAWinningThreeCardTwentyOnePaysTwoToOne() {
        // bet 4: 6 + 4 + floor(4 x 2.0) = 18
        assertEquals(18, threeCardTwentyOneBeatsNineteen(4));
    }

    @Test
    public void payout21SetsWhatAWinningTwentyOnePays() {
        // the pair of the test above, payout21 1.5: 6 + 4 + floor(4 x 1.5) = 16
        params.setParameterValue("payout21", 1.5);
        assertEquals(16, threeCardTwentyOneBeatsNineteen(4));
    }

    @Test
    public void theTwentyOnePayoutIsRoundedDown() {
        // payout21 1.2, bet 8: 2 + 8 + floor(9.6) = 19
        params.setParameterValue("payout21", 1.2);
        assertEquals(19, threeCardTwentyOneBeatsNineteen(8));
    }

    @Test
    public void withNaturalOnlyAWinningThreeCardTwentyOnePaysEvenMoney() {
        // the pair of payout21SetsWhatAWinningTwentyOnePays (payout21 1.5 in both): 6 + 4 + 4 = 14
        pagatNaturals(params);
        assertEquals(14, threeCardTwentyOneBeatsNineteen(4));
    }

    @Test
    public void byDefaultAWinningNaturalIsPaidTwoToOneAfterThePlayerStands() {
        // bet 2 on A + K against 9 + 10 = 19: 8 + 2 + 4 = 14
        newState(1);
        arrangePlay(state, new int[]{2}, new String[]{"AH KC"}, "9S", "10D", "");
        assertTrue(state.isNotTerminal());
        assertEquals(8, state.getChips(0));
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(14, state.getChips(0));
        assertArrayEquals(new Object[]{WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void byDefaultATwentyOnePaysTwoToOneWhenTheDealerBusts() {
        // dealer 6 + 10 = 16 draws the K: bust. Player 0's 21 (10 + 5 + 6, bet 4) wins 8: 6 + 4 + 8 = 18;
        // player 1's 17 (bet 2) wins even money: 8 + 2 + 2 = 12
        newState(2);
        arrangePlay(state, new int[]{4, 2}, new String[]{"10C 5C", "9H 8D"}, "6S", "10D", "6H KH");
        fm.next(state, new Hit());
        fm.next(state, new Stand());
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("6S 10D KH"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{18, 12}, state.chips);
    }

    @Test
    public void byDefaultATwentyOneThatTiesTheDealersTwentyOneIsAPush() {
        // dealer 6 + 5 = 11 draws the K: 21 from three cards (no Blackjack: there is no peek under a 6).
        // Player 0's natural (bet 2) and player 1's 10 + 5 + 6 (bet 4) both push
        newState(2);
        arrangePlay(state, new int[]{2, 4}, new String[]{"AH KC", "10C 5C"}, "6S", "5D", "6H KH");
        fm.next(state, new Stand());
        fm.next(state, new Hit());
        fm.next(state, new Stand());
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("6S 5D KH"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{10, 10}, state.chips);
        assertArrayEquals(new Object[]{DRAW_GAME, DRAW_GAME}, state.getPlayerResults());
    }
}
