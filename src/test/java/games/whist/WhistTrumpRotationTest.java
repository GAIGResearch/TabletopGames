package games.whist;

import core.components.FrenchCard;
import core.components.FrenchCard.Suite;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.components.FrenchCard.Suite.*;
import static games.whist.WhistParameters.TrumpMode.ROTATION;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

/**
 * trumpMode ROTATION: the trump suit of deal d (d = round counter, from 0) is [Hearts, Diamonds, Spades, Clubs][d % 4],
 * or with noTrumpsInRotation [Hearts, Diamonds, Spades, Clubs, none][d % 5]. No card is turned up. Deals are ended
 * by an arranged last trick played through fm.next (WhistTestUtils.playLastTrick).
 */
public class WhistTrumpRotationTest {

    WhistForwardModel fm = new WhistForwardModel();

    /**
     * The trump suit and trump card at the start of each deal, playing nDeals deals (all but the last to the end).
     */
    private List<Suite> trumpsByDeal(WhistGameState state, int nDeals) {
        List<Suite> trumps = new ArrayList<>();
        for (int d = 0; d < nDeals; d++) {
            assertEquals(d, state.getRoundCounter());
            assertNull("no card is turned up in deal " + d, state.getTrumpCard());
            trumps.add(state.getTrumpSuit());
            if (d < nDeals - 1)
                playLastTrick(state, fm, new int[]{3, 3, 3, 3}, d % 4);
        }
        return trumps;
    }

    @Test
    public void trumpsRotateHeartsDiamondsSpadesClubsAndBackToHearts() {
        WhistGameState state = newState(21, params(6, ROTATION, false));
        // deals 0-5: d % 4 = 0, 1, 2, 3, 0, 1
        assertEquals(Arrays.asList(Hearts, Diamonds, Spades, Clubs, Hearts, Diamonds), trumpsByDeal(state, 6));
    }

    @Test
    public void withNoTrumpsInRotationTheFifthDealHasNoTrumps() {
        WhistGameState state = newState(22, params(6, ROTATION, true));
        // deals 0-5: d % 5 = 0, 1, 2, 3, 4 (no trumps), 0
        assertEquals(Arrays.asList(Hearts, Diamonds, Spades, Clubs, null, Hearts), trumpsByDeal(state, 6));
    }

    @Test
    public void inTheHeartsDealAHeartBeatsAHigherCardOfTheSuitLed() {
        WhistGameState state = newState(23, params(1, ROTATION, false));
        assertEquals(Hearts, state.getTrumpSuit());
        // two cards each, so the trick is not the last. Player 0 leads the King of Clubs; player 1, void in clubs,
        // plays the 2 of Hearts; the Ace of Clubs is the highest club but the heart is a trump
        arrangeLastTrick(state, 0, new int[]{3, 2, 3, 3}, "KC", "2H", "AC", "3C");
        giveHand(state, 0, "KC", "4D");
        giveHand(state, 1, "2H", "5D");
        giveHand(state, 2, "AC", "6D");
        giveHand(state, 3, "3C", "7D");
        playCards(state, fm, "KC", "2H", "AC", "3C");

        assertArrayEquals(new int[]{3, 3, 3, 3}, state.tricksTaken);
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void withNoTrumpsTheHighestCardOfTheSuitLedWins() {
        WhistGameState state = newState(24, params(6, ROTATION, true));
        for (int d = 0; d < 4; d++)
            playLastTrick(state, fm, new int[]{3, 3, 3, 3}, 0);
        assertEquals(4, state.getRoundCounter());
        assertNull(state.getTrumpSuit());
        // player 0 leads the 5 of Clubs; the Ace of Hearts (hearts are trumps in the first deal) and the King of
        // Spades are higher but off-suit, so player 2's 9 of Clubs wins
        arrangeLastTrick(state, 0, new int[]{3, 2, 3, 3}, "5C", "AH", "9C", "KS");
        giveHand(state, 0, "5C", "4D");
        giveHand(state, 1, "AH", "5D");
        giveHand(state, 2, "9C", "6D");
        giveHand(state, 3, "KS", "7D");
        playCards(state, fm, "5C", "AH", "9C", "KS");

        assertArrayEquals(new int[]{3, 2, 4, 3}, state.tricksTaken);
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void noCardIsTurnedUpSoTheDealersWholeHandIsRedeterminised() {
        WhistGameState state = newState(25, params(1, ROTATION, false));
        assertNull(state.getTrumpCard());
        List<FrenchCard> dealerHand = new ArrayList<>(state.getPlayerHand(3).getComponents());
        // seen by player 1, every card of dealer 3's hand is sometimes given to another player
        List<FrenchCard> neverMoved = new ArrayList<>(dealerHand);
        for (int i = 0; i < 30; i++) {
            WhistGameState copy = (WhistGameState) state.copy(1);
            neverMoved.removeIf(c -> !copy.getPlayerHand(3).contains(c));
        }
        assertTrue("always left with the dealer: " + neverMoved, neverMoved.isEmpty());
    }
}
