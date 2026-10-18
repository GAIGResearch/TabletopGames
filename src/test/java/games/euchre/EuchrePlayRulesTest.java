package games.euchre;

import org.junit.Before;
import org.junit.Test;

import static core.components.FrenchCard.Suite.Diamonds;
import static games.euchre.EuchreTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.*;
import static org.junit.Assert.*;

/**
 * The play of a trick: follow the suit led, the trick winner and the next lead. The standard deal with Diamonds
 * called in round 2 by player 1 (the red Jacks, the bowers, are in the kitty):
 * <pre>
 * P0: 9S 10S JS AH 9D
 * P1: QS KS AS 10H 10D
 * P2: QD AD QH QC 10C
 * P3: KD KH AC KC JC   (dealer)
 * </pre>
 */
public class EuchrePlayRulesTest {

    EuchreGameState state;
    EuchreForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new EuchreForwardModel();
        standardDeal(state);
        startPlay(state, Diamonds, 1, null);
        assertEquals(0, state.getCurrentPlayer());
    }

    @Test
    public void theLeaderMayPlayAnyCard() {
        assertEquals(plays("9S", "10S", "JS", "AH", "9D"), available(state, fm));
    }

    @Test
    public void aFollowerHoldingTheSuitLedMustFollowIt() {
        arrangeTrick(state, 0, "9S");
        assertEquals(1, state.getCurrentPlayer());
        // spades led: not the trump 10D nor the 10H
        assertEquals(plays("QS", "KS", "AS"), available(state, fm));
    }

    @Test
    public void aFollowerVoidInTheSuitLedMayPlayAnyCard() {
        arrangeTrick(state, 0, "9S", "KS");
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(plays("QD", "AD", "QH", "QC", "10C"), available(state, fm));
    }

    @Test
    public void aFollowerMustFollowTheSuitLedEvenAfterATrump() {
        // dealer 3 led clubs, player 0 discarded, player 1 trumped: player 2 must still follow clubs
        arrangeTrick(state, 3, "KC", "9S", "10D");
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(plays("QC", "10C"), available(state, fm));
    }

    @Test
    public void theHighestTrumpWinsAndLeadsTheNextTrick() {
        arrangeTrick(state, 0, "9S", "AS", "QD");
        playCards(state, fm, "KD");
        // spades led; player 2 trumped with QD, player 3 over-trumped with KD (13 > 12): player 3 wins
        assertArrayEquals(new int[]{0, 0, 0, 1}, state.tricksTaken);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(3, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(4, state.getDiscardPile().getSize());
        for (String c : new String[]{"9S", "AS", "QD", "KD"})
            assertTrue(c + " on the discard pile", state.getDiscardPile().contains(card(c)));
        assertAllCardsPresent(state);
    }

    @Test
    public void withNoTrumpPlayedTheHighestCardOfTheSuitLedWins() {
        arrangeTrick(state, 0, "10S", "QS", "10C");
        playCards(state, fm, "AC");
        // spades led, no Diamonds: QS is the highest spade; the Ace of Clubs is off-suit and cannot win
        assertArrayEquals(new int[]{0, 1, 0, 0}, state.tricksTaken);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getCurrentTrick().getLeader());
    }

    @Test
    public void theLowestTrumpBeatsTheAceLed() {
        arrangeTrick(state, 3, "AC", "9D", "AS");
        playCards(state, fm, "QC");
        // clubs led by the dealer; player 0's 9D is the only trump: player 0 wins
        assertArrayEquals(new int[]{1, 0, 0, 0}, state.tricksTaken);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getLeader());
    }
}
